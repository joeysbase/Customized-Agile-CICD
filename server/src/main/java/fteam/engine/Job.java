package fteam.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.time.OffsetDateTime;

public class Job implements Runnable {

  private String name;
  private String stage;
  private List<String> needs;
  private String image;
  private List<String> scripts;
  private volatile OffsetDateTime startTime;
  private volatile OffsetDateTime endTime;

  // ========== Execution status ==========
  private volatile JobStatus status = JobStatus.PENDING;
  private volatile String errorMessage;
  private final CountDownLatch completionLatch = new CountDownLatch(1);

  // ========== Execution context ==========
  private File repoDir;
  private JobExecutionCallback callback;

  public enum JobStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
  }

  // ========== Callback interface ==========
  public interface JobExecutionCallback {
    void onMessage(String message);
  }

  private Job(Builder builder) {
    this.name = builder.name;
    this.stage = builder.stage;
    this.needs = builder.needs;
    this.image = builder.image;
    this.scripts = builder.scripts;
  }

  // ========== Runnable implementation ==========
  @Override
  public void run() {
    try {
      startTime = OffsetDateTime.now();   // ✅ job start
      status = JobStatus.RUNNING;
      log("[" + Thread.currentThread().getName() + "] Starting: " + name);

      executeJob();

      status = JobStatus.SUCCESS;
      log("[" + Thread.currentThread().getName() + "] Completed: " + name);
    } catch (Exception e) {
      status = JobStatus.FAILED;
      errorMessage = e.getMessage();
      log("[" + Thread.currentThread().getName() + "] Failed: " + name + " - " + errorMessage);
    } finally {
      endTime = OffsetDateTime.now();     // ✅ job end
      completionLatch.countDown();
    }
  }

  /**
   * Execute the job - runs all scripts
   */
  private void executeJob() throws Exception {
    if (scripts == null || scripts.isEmpty()) {
      log("WARNING: job has empty script, treat as success.");
      return;
    }

    log("Image: " + image);
    log("Scripts:");

    for (String cmd : scripts) {
      log("$ " + cmd);
      int exitCode = runCommand(cmd);
      if (exitCode != 0) {
        throw new Exception("Command failed with exit code: " + exitCode);
      }
    }
  }

  /**
   * Run a single command using ProcessBuilder
   */
  private int runCommand(String cmd) {
    try {
      ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
      if (repoDir != null) {
        pb.directory(repoDir);
      }
      pb.redirectErrorStream(true);

      Process p = pb.start();

      try (BufferedReader br =
          new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line;
        while ((line = br.readLine()) != null) {
          log(line);
        }
      }

      return p.waitFor();

    } catch (Exception e) {
      log("ERROR: exception while executing command: " + e.getMessage());
      return 1;
    }
  }

  /**
   * Log message via callback or to console
   */
  private void log(String message) {
    if (callback != null) {
      callback.onMessage(message);
    } else {
      System.out.println(message);
    }
  }

  // ========== Synchronization methods ==========
  public void waitForCompletion() throws InterruptedException {
    completionLatch.await();
  }

  public boolean isCompleted() {
    return status == JobStatus.SUCCESS || status == JobStatus.FAILED;
  }

  public boolean isSuccess() {
    return status == JobStatus.SUCCESS;
  }

  public boolean isFailed() {
    return status == JobStatus.FAILED;
  }

  // ========== Setters for execution context ==========
  public void setRepoDir(File repoDir) {
    this.repoDir = repoDir;
  }

  public void setCallback(JobExecutionCallback callback) {
    this.callback = callback;
  }

  // ========== Factory method for YAML parsing ==========
  public static Job fromYaml(String jobName, Map<String, Object> jobMap) {
    String stage = (String) jobMap.get("stage");
    String image = (String) jobMap.get("image");

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

    return new Job.Builder(jobName, stage, image, script)
        .needs(needs)
        .build();
  }

  // ========== Builder ==========
  static class Builder {
    private String name;
    private String stage;
    private List<String> needs = null;
    private String image;
    private List<String> scripts;

    Builder(String name, String stage, String image, List<String> scripts) {
      this.name = name;
      this.stage = stage;
      this.image = image;
      this.scripts = scripts;
    }

    Builder needs(List<String> needs) {
      this.needs = needs;
      return this;
    }

    Job build() {
      return new Job(this);
    }
  }

  // ========== Getters ==========
  public String getName() {
    return this.name;
  }

  public String getStage() {
    return this.stage;
  }

  public List<String> getNeeds() {
    return this.needs;
  }

  public String getImage() {
    return this.image;
  }

  public String[] getScript() {
    return scripts.toArray(new String[0]);
  }

  public List<String> getScripts() {
    return this.scripts;
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }

  public JobStatus getStatus() {
    return this.status;
  }
  public OffsetDateTime getStartTime() { return startTime; }
  public OffsetDateTime getEndTime() { return endTime; }
}