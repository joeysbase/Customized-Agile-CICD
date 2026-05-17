package fteam.engine;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.io.File;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Worker that validates a pipeline configuration and executes it against a repository. */
public class RunWorker extends Worker {

  private static final Logger logger = LoggerFactory.getLogger(RunWorker.class);
  private static final String STATUS_SUCCESS = "success";
  private static final String STATUS_FAILED = "failed";
  private static final String SEPARATOR = "========================================";

  private final String configurationString;
  private final File repoDir;
  private String gitBranch = "";
  private String gitCommit = "";

  private RunWorker(String configStr, File repoDir) {
    this.configurationString = configStr;
    this.repoDir = repoDir;
  }

  /**
   * Creates a run worker from inline YAML content and a prepared repository directory.
   *
   * @param fileString pipeline configuration content
   * @param repoDir repository directory to execute against
   * @return configured run worker
   */
  public static RunWorker fromFileString(String fileString, File repoDir) {
    return new RunWorker(fileString, repoDir);
  }

  /**
   * Sets Git metadata that will be persisted with the pipeline run.
   *
   * @param branch Git branch name
   * @param commit Git commit identifier
   */
  public void setGitInfo(String branch, String commit) {
    this.gitBranch = (branch == null) ? "" : branch;
    this.gitCommit = (commit == null) ? "" : commit;
  }

  /** Executes the configured pipeline and records the resulting run, stage, and job state. */
  @Override
  public void run() {
    PipelineConfig cfg = PipelineConfig.fromFile(configurationString);

    if (!cfg.isvalidConfig()) {
      addMessage("ERROR: Invalid pipeline configuration");
      for (String s : cfg.getVerificationMsg()) {
        addMessage("  - " + s);
      }
      setWorkDone();
      return;
    }

    if (!KubernetesJobExecutor.isInCluster()
        && (repoDir == null || !repoDir.exists() || !repoDir.isDirectory())) {
      addMessage("ERROR: Repository directory is invalid: " + repoDir);
      setWorkDone();
      return;
    }

    DataStoreAgent ds = DataStoreAgent.getInstance();

    String pipeline = cfg.getName();
    int runNoNum = ds.nextRunNo(pipeline);

    MDC.put("pipeline", pipeline);
    MDC.put("run_no", String.valueOf(runNoNum));
    MDC.put("source", "system");

    Tracer tracer = GlobalOpenTelemetry.getTracer("cicd-server");
    Span pipelineSpan =
        tracer
            .spanBuilder("pipeline:" + pipeline)
            .setAttribute(AttributeKey.stringKey("pipeline"), pipeline)
            .setAttribute(AttributeKey.longKey("run_no"), (long) runNoNum)
            .startSpan();
    try (Scope pipelineScope = pipelineSpan.makeCurrent()) {
      String traceId = pipelineSpan.getSpanContext().getTraceId();

      OffsetDateTime pipelineStart = OffsetDateTime.now();
      ds.startRun(pipeline, runNoNum, pipelineStart, "", gitBranch, gitCommit);
      ds.setTraceId(pipeline, runNoNum, traceId);

      logger.info("Starting pipeline: {} run: {} traceId: {}", pipeline, runNoNum, traceId);
      addMessage("Run-No: " + runNoNum);

      addMessage(SEPARATOR);
      addMessage("Starting Pipeline Execution");
      addMessage(SEPARATOR);
      addMessage("Pipeline: " + cfg.getName());
      if (cfg.getDescription() != null && !cfg.getDescription().isEmpty()) {
        addMessage("Description: " + cfg.getDescription());
      }
      if (repoDir != null) {
        addMessage("Repository: " + repoDir.getAbsolutePath());
      }
      addMessage("");

      Job.JobExecutionCallback callback = this::addMessage;

      // k8s mode: create PVC for workspace sharing between jobs
      KubernetesJobExecutor k8sExecutor = null;
      String pvcName = null;
      if (KubernetesJobExecutor.isInCluster()) {
        try {
          k8sExecutor = new KubernetesJobExecutor();
          pvcName = k8sExecutor.createWorkspacePvc("run-" + runNoNum);
          addMessage("Created k8s workspace PVC: " + pvcName);
        } catch (Exception e) {
          addMessage("ERROR: Failed to create workspace PVC: " + e.getMessage());
          setWorkDone();
          return;
        }
      }

      JobScheduler scheduler =
          (pvcName != null)
              ? new JobScheduler(repoDir, pvcName, callback)
              : new JobScheduler(repoDir, callback);

      int totalJobs = cfg.getExcutionSequence().size();
      int successCount = 0;
      int failedCount = 0;
      int allowedFailureCount = 0;
      boolean pipelineFailed = false;

      try {
        List<String> stages = cfg.getStagesInOrder();
        addMessage("Total stages: " + stages.size());
        addMessage("Total jobs: " + totalJobs);
        addMessage("");

        for (String stageName : stages) {
          List<Job> stageJobs =
              cfg.getJobs().stream()
                  .filter(j -> j.getStage().equals(stageName))
                  .collect(Collectors.toList());

          if (stageJobs.isEmpty()) {
            addMessage("Skipping empty stage: " + stageName);
            continue;
          }

          for (Job job : stageJobs) {
            job.setPipelineName(pipeline);
          }

          OffsetDateTime stageStart = OffsetDateTime.now();
          ds.startStage(pipeline, runNoNum, stageName, stageStart);

          addMessage("Executing stage: " + stageName + " (" + stageJobs.size() + " jobs)");

          boolean stageSuccess = false;
          OffsetDateTime stageEnd;

          try {
            stageSuccess = scheduler.executeStage(stageName, stageJobs);
          } finally {
            stageEnd = OffsetDateTime.now();

            boolean hasBlockingFailure = false;

            for (Job job : stageJobs) {
              String jobStatus = job.isSuccess() ? STATUS_SUCCESS : STATUS_FAILED;

              if (job.isSuccess()) {
                successCount++;
              } else if (job.isFailed()) {
                failedCount++;

                if (job.isFailureAllowed()) {
                  allowedFailureCount++;
                  addMessage(
                      "WARNING: Job `" + job.getName() + "` failed but is allowed to fail.");
                } else {
                  hasBlockingFailure = true;
                }
              }

              OffsetDateTime jobStart = null;
              OffsetDateTime jobEnd = null;
              try {
                jobStart = job.getStartTime();
                jobEnd = job.getEndTime();
              } catch (RuntimeException ignore) {
                logger.debug("Failed to read job timestamps for {}", job.getName(), ignore);
              }

              ds.upsertJob(
                  pipeline,
                  runNoNum,
                  stageName,
                  job.getName(),
                  jobStart,
                  jobEnd,
                  jobStatus,
                  job.getErrorMessage(),
                  job.isFailureAllowed());
            }

            stageSuccess = !hasBlockingFailure;

            ds.finishStage(
                pipeline,
                runNoNum,
                stageName,
                stageEnd,
                stageSuccess ? STATUS_SUCCESS : STATUS_FAILED);

            double stageDurationSec =
                Duration.between(stageStart, stageEnd).toMillis() / 1000.0;
            MetricsRegistry.getInstance()
                .recordStageDuration(pipeline, stageName, stageDurationSec);

            MDC.put("stage", stageName);
            MDC.put("duration_sec", String.format("%.3f", stageDurationSec));
            MDC.put("status", stageSuccess ? STATUS_SUCCESS : STATUS_FAILED);
            logger.info(
                "Stage {} finished: {}",
                stageName,
                stageSuccess ? STATUS_SUCCESS : STATUS_FAILED);
            MDC.remove("stage");
            MDC.remove("duration_sec");
            MDC.remove("status");
          }

          addMessage("");

          if (!stageSuccess) {
            addMessage("✗ Stage " + stageName + " FAILED");
            addMessage("Stopping pipeline execution (fail-fast mode)");
            pipelineFailed = true;
            break;
          }
        }

        if (!pipelineFailed) {
          addMessage(SEPARATOR);
          addMessage("✓ Pipeline Completed Successfully");
          addMessage(SEPARATOR);
        } else {
          addMessage(SEPARATOR);
          addMessage("✗ Pipeline FAILED");
          addMessage(SEPARATOR);
        }

      } catch (Exception e) {
        addMessage("ERROR: Pipeline execution failed with exception");
        addMessage("  " + e.getMessage());
        logger.error("Pipeline execution failed", e);
        pipelineFailed = true;

      } finally {
        scheduler.shutdown();

        addMessage("");
        addMessage("Execution Summary:");
        addMessage("  Total jobs:            " + totalJobs);
        addMessage("  Succeeded:             " + successCount);
        addMessage("  Failed:                " + failedCount);
        addMessage("  Allowed failures:      " + allowedFailureCount);
        int skippedCount = totalJobs - successCount - failedCount;
        addMessage("  Skipped:               " + skippedCount);

        OffsetDateTime pipelineEnd = OffsetDateTime.now();
        String pipelineStatus = pipelineFailed ? STATUS_FAILED : STATUS_SUCCESS;
        ds.finishRun(pipeline, runNoNum, pipelineEnd, pipelineStatus);

        double durationSec = Duration.between(pipelineStart, pipelineEnd).toMillis() / 1000.0;
        MetricsRegistry.getInstance().recordPipelineRun(pipeline, pipelineStatus, durationSec);

        // Cleanup k8s workspace PVC
        if (k8sExecutor != null && pvcName != null) {
          k8sExecutor.deleteWorkspacePvc(pvcName);
          addMessage("Deleted k8s workspace PVC: " + pvcName);
        }

        if (pipelineFailed) {
          pipelineSpan.setStatus(StatusCode.ERROR, "Pipeline failed");
        } else {
          pipelineSpan.setStatus(StatusCode.OK);
        }
        pipelineSpan.end();

        MDC.put("branch", gitBranch);
        MDC.put("commit", gitCommit);
        MDC.put("duration_sec", String.format("%.3f", durationSec));
        MDC.put("status", pipelineStatus);
        MDC.put("trace_id", traceId);
        logger.info("Pipeline {} run {} finished: {}", pipeline, runNoNum, pipelineStatus);
        MDC.remove("pipeline");
        MDC.remove("run_no");
        setWorkDone();
      }
    }
  }
}
