package fteam.engine;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Represents a single executable job inside a pipeline stage. */
public class Job implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(Job.class);
  private static final String FAILURES_KEY = "failures";

  private String name;
  private String stage;
  private List<String> needs;
  private String image;
  private List<String> scripts;
  private volatile OffsetDateTime startTime;
  private volatile OffsetDateTime endTime;
  private boolean failures;

  // ========== Execution status ==========
  private volatile JobStatus status = JobStatus.PENDING;
  private volatile String errorMessage;
  private final CountDownLatch completionLatch = new CountDownLatch(1);

  // ========== Execution context ==========
  private File repoDir;
  private String pvcName;
  private String pipelineName;
  private JobExecutionCallback callback;

  /** Execution state for a job lifecycle. */
  public enum JobStatus {
    /** Job has been created but has not started yet. */
    PENDING,
    /** Job is currently executing. */
    RUNNING,
    /** Job finished successfully. */
    SUCCESS,
    /** Job finished with a failure. */
    FAILED
  }

  // ========== Callback interface ==========
  /** Callback used to stream job output back to the caller. */
  public interface JobExecutionCallback {
    /**
     * Publishes one line of job output.
     *
     * @param message message to publish
     */
    void onMessage(String message);
  }

  private Job(Builder builder) {
    this.name = builder.name;
    this.stage = builder.stage;
    this.needs = builder.requiredJobs;
    this.image = builder.image;
    this.scripts = builder.scripts;
    this.failures = builder.failureAllowed;
  }

  // ========== Runnable implementation ==========
  /** Executes the job and records lifecycle, tracing, and metrics information. */
  @Override
  public void run() {
    MDC.put("stage", stage);
    MDC.put("job", name);

    Tracer tracer = GlobalOpenTelemetry.getTracer("cicd-server");
    Span jobSpan = tracer.spanBuilder("job:" + name).startSpan();
    Scope jobScope = jobSpan.makeCurrent();

    try {
      startTime = OffsetDateTime.now();
      status = JobStatus.RUNNING;
      logger.info("Starting job: {}", name);

      executeJob();

      status = JobStatus.SUCCESS;
      jobSpan.setStatus(StatusCode.OK);
    } catch (Exception e) {
      status = JobStatus.FAILED;
      errorMessage = e.getMessage();
      jobSpan.setStatus(StatusCode.ERROR, errorMessage);
      jobSpan.recordException(e);
    } finally {
      endTime = OffsetDateTime.now();
      String jobStatus = (status == JobStatus.SUCCESS) ? "success" : "failed";
      if (pipelineName != null && startTime != null) {
        double durationSec = Duration.between(startTime, endTime).toMillis() / 1000.0;
        MDC.put("duration_sec", String.format("%.3f", durationSec));
        MDC.put("status", jobStatus);
        MetricsRegistry.getInstance()
            .recordJobRun(pipelineName, stage, name, jobStatus, durationSec);
      }
      if (status == JobStatus.SUCCESS) {
        logger.info("Completed job: {}", name);
      } else {
        logger.error("Failed job: {} - {}", name, errorMessage);
      }
      jobSpan.end();
      jobScope.close();
      completionLatch.countDown();
      MDC.remove("stage");
      MDC.remove("job");
    }
  }

  /** Executes the configured script list for this job. */
  private void executeJob() throws Exception {
    if (scripts == null || scripts.isEmpty()) {
      log("WARNING: job has empty script, treat as success.");
      return;
    }

    if (image != null && !image.isBlank()) {
      int exitCode;
      if (KubernetesJobExecutor.isInCluster() && pvcName != null) {
        log("Running job as k8s Job: " + image);
        KubernetesJobExecutor executor = new KubernetesJobExecutor();
        exitCode = executor.runJob(image, scripts, pvcName, name);
      } else {
        log("Running job in Docker image: " + image);
        DockerJobExecutor executor = new DockerJobExecutor();
        exitCode = executor.runJob(image, scripts, repoDir);
      }
      if (exitCode != 0) {
        throw new Exception("Command failed with exit code: " + exitCode);
      }
      return;
    }

    log("No image specified; falling back to local execution.");
    for (String cmd : scripts) {
      log("$ " + cmd);
      int exitCode = runCommand(cmd);
      if (exitCode != 0) {
        throw new Exception("Command failed with exit code: " + exitCode);
      }
    }
  }

  /** Runs a single command with {@link ProcessBuilder}. */
  private int runCommand(String cmd) {
    try {
      ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
      if (repoDir != null) {
        pb.directory(repoDir);
      }
      pb.redirectErrorStream(true);

      Process p = pb.start();

      try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line = br.readLine();
        while (line != null) {
          log(line);
          line = br.readLine();
        }
      }

      return p.waitFor();

    } catch (Exception e) {
      log("ERROR: exception while executing command: " + e.getMessage());
      return 1;
    }
  }

  /** Logs a message via the callback and SLF4J. */
  private void log(String message) {
    if (callback != null) {
      callback.onMessage(message);
    }
    logger.info("{}", message);
  }

  // ========== Synchronization methods ==========
  /**
   * Blocks until this job completes.
   *
   * @throws InterruptedException if the waiting thread is interrupted
   */
  public void waitForCompletion() throws InterruptedException {
    completionLatch.await();
  }

  /**
   * Indicates whether the job has reached a terminal state.
   *
   * @return {@code true} when the job has succeeded or failed
   */
  public boolean isCompleted() {
    return status == JobStatus.SUCCESS || status == JobStatus.FAILED;
  }

  /**
   * Indicates whether the job completed successfully.
   *
   * @return {@code true} when the job status is success
   */
  public boolean isSuccess() {
    return status == JobStatus.SUCCESS;
  }

  /**
   * Indicates whether the job failed.
   *
   * @return {@code true} when the job status is failed
   */
  public boolean isFailed() {
    return status == JobStatus.FAILED;
  }

  // ========== Setters for execution context ==========
  /**
   * Sets the repository directory used for local or containerized execution.
   *
   * @param repoDir repository directory
   */
  public void setRepoDir(File repoDir) {
    this.repoDir = repoDir;
  }

  /**
   * Sets the PVC name used by Kubernetes-backed execution.
   *
   * @param pvcName workspace PVC name
   */
  public void setPvcName(String pvcName) {
    this.pvcName = pvcName;
  }

  /**
   * Sets the pipeline name used for metrics and logging.
   *
   * @param pipelineName pipeline name
   */
  public void setPipelineName(String pipelineName) {
    this.pipelineName = pipelineName;
  }

  /**
   * Sets the callback used to publish job output.
   *
   * @param callback callback implementation
   */
  public void setCallback(JobExecutionCallback callback) {
    this.callback = callback;
  }

  // ========== Factory method for YAML parsing ==========
  /**
   * Builds a job instance from a parsed YAML mapping.
   *
   * @param jobName job name
   * @param jobMap YAML mapping for the job
   * @return parsed job instance
   */
  public static Job fromYaml(String jobName, Map<String, Object> jobMap) {
    // script: String or List<String>
    Object scriptObj = jobMap.get("script");
    List<String> script = new ArrayList<>();

    switch (scriptObj) {
      case String string -> script.add(string);
      case List<?> list -> {
        for (Object o : list) {
          script.add((String) o);
        }
      }
      default -> {
      }
    }

    // needs: optional
    List<String> needs = new ArrayList<>();
    Object needsObj = jobMap.get("needs");
    if (needsObj instanceof List<?> list) {
      for (Object o : list) {
        needs.add((String) o);
      }
    }
    Object failuresObj = jobMap.get(FAILURES_KEY);
    boolean failures = false;

    if (failuresObj != null) {
      if (!(failuresObj instanceof Boolean b)) {
        throw new IllegalArgumentException("`" + FAILURES_KEY + "` must be boolean true/false");
      }
      failures = b;
    }

    String stage = (String) jobMap.get("stage");
    String image = (String) jobMap.get("image");
    return new Job.Builder(jobName, stage, image, script).needs(needs).failures(failures).build();
  }

  // ========== Builder ==========
  static class Builder {
    private String name;
    private String stage;
    private List<String> requiredJobs = null;
    private String image;
    private List<String> scripts;
    private boolean failureAllowed = false;

    Builder(String name, String stage, String image, List<String> scripts) {
      this.name = name;
      this.stage = stage;
      this.image = image;
      this.scripts = scripts;
    }

    Builder needs(List<String> needs) {
      this.requiredJobs = needs;
      return this;
    }

    Builder failures(boolean failures) {
      this.failureAllowed = failures;
      return this;
    }

    Job build() {
      return new Job(this);
    }
  }

  // ========== Getters ==========
  /**
   * Returns the job name.
   *
   * @return job name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns the stage this job belongs to.
   *
   * @return stage name
   */
  public String getStage() {
    return this.stage;
  }

  /**
   * Returns the in-stage dependencies required before this job can start.
   *
   * @return dependency list
   */
  public List<String> getNeeds() {
    return this.needs;
  }

  /**
   * Returns the execution image configured for this job.
   *
   * @return container image name
   */
  public String getImage() {
    return this.image;
  }

  /**
   * Returns the configured script lines as an array.
   *
   * @return script lines
   */
  public String[] getScript() {
    return scripts.toArray(new String[0]);
  }

  /**
   * Returns the configured script lines as a list.
   *
   * @return script lines
   */
  public List<String> getScripts() {
    return this.scripts;
  }

  /**
   * Returns the last recorded error message for this job.
   *
   * @return error message or {@code null}
   */
  public String getErrorMessage() {
    return this.errorMessage;
  }

  /**
   * Indicates whether this job is allowed to fail without failing the pipeline.
   *
   * @return {@code true} when failure is allowed
   */
  public boolean isFailureAllowed() {
    return failures;
  }

  /**
   * Returns the current job status.
   *
   * @return job status
   */
  public JobStatus getStatus() {
    return this.status;
  }

  /**
   * Returns the job start time.
   *
   * @return start time or {@code null}
   */
  public OffsetDateTime getStartTime() {
    return startTime;
  }

  /**
   * Returns the job end time.
   *
   * @return end time or {@code null}
   */
  public OffsetDateTime getEndTime() {
    return endTime;
  }
}
