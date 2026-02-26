
package fteam.engine;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RunWorker extends Worker {

  private final String configurationString;
  private final File repoDir;

  private RunWorker(String configStr, File repoDir) {
    this.configurationString = configStr;
    this.repoDir = repoDir;
  }

  public static RunWorker fromFileString(String fileString, File repoDir) {
    return new RunWorker(fileString, repoDir);
  }

  @Override
  public void run() {
    // ========== 1. Parse and validate configuration ==========
    PipelineConfig cfg = PipelineConfig.fromFile(configurationString);

    if (!cfg.isvalidConfig()) {
      addMessage("ERROR: Invalid pipeline configuration");
      for (String s : cfg.getVerificationMsg()) {
        addMessage("  - " + s);
      }
      setWorkDone();
      return;
    }

    // ========== 2. Validate repository directory ==========
    if (repoDir == null || !repoDir.exists() || !repoDir.isDirectory()) {
      addMessage("ERROR: Repository directory is invalid: " + repoDir);
      setWorkDone();
      return;
    }

    // ========== 3. Init datastore + create a run ==========
    DataStoreAgent ds = DataStoreAgent.getInstance();

    String pipeline = cfg.getName();
    int runNoNum = ds.nextRunNo(pipeline);

    OffsetDateTime pipelineStart = OffsetDateTime.now();
    ds.startRun(pipeline, runNoNum, pipelineStart, "", "", "");

    addMessage("Run-No: " + runNoNum);

    // ========== 4. Print pipeline information ==========
    addMessage("========================================");
    addMessage("Starting Pipeline Execution");
    addMessage("========================================");
    addMessage("Pipeline: " + cfg.getName());
    if (cfg.getDescription() != null && !cfg.getDescription().isEmpty()) {
      addMessage("Description: " + cfg.getDescription());
    }
    addMessage("Repository: " + repoDir.getAbsolutePath());
    addMessage("");

    // ========== 5. Create callback for job logging ==========
    Job.JobExecutionCallback callback = this::addMessage;

    // ========== 6. Create JobScheduler ==========
    JobScheduler scheduler = new JobScheduler(repoDir, callback);

    // ========== 7. Execute pipeline stage by stage ==========
    int totalJobs = cfg.getExcutionSequence().size();
    int successCount = 0;
    int failedCount = 0;
    boolean pipelineFailed = false;

    try {
      List<String> stages = cfg.getStagesInOrder();
      addMessage("Total stages: " + stages.size());
      addMessage("Total jobs: " + totalJobs);
      addMessage("");

      for (String stageName : stages) {
        List<Job> stageJobs = cfg.getJobs().stream()
            .filter(j -> j.getStage().equals(stageName))
            .collect(Collectors.toList());

        if (stageJobs.isEmpty()) {
          addMessage("Skipping empty stage: " + stageName);
          continue;
        }

        // ===== datastore: stage start =====
        OffsetDateTime stageStart = OffsetDateTime.now();
        ds.startStage(pipeline, runNoNum, stageName, stageStart);

        addMessage("Executing stage: " + stageName + " (" + stageJobs.size() + " jobs)");

        boolean stageSuccess = false;
        OffsetDateTime stageEnd;

        try {
          stageSuccess = scheduler.executeStage(stageName, stageJobs);
        } finally {
          stageEnd = OffsetDateTime.now();

          for (Job job : stageJobs) {
            String jobStatus = job.isSuccess() ? "success" : (job.isFailed() ? "failed" : "failed");

            OffsetDateTime jobStart = null;
            OffsetDateTime jobEnd = null;
            try {
              jobStart = job.getStartTime();
              jobEnd = job.getEndTime();
            } catch (Throwable ignore) {
            }

            ds.upsertJob(
                pipeline,
                runNoNum,
                stageName,
                job.getName(),
                jobStart,
                jobEnd,
                jobStatus,
                job.getErrorMessage()
            );
          }

          // ===== datastore: stage end =====
          ds.finishStage(pipeline, runNoNum, stageName, stageEnd, stageSuccess ? "success" : "failed");
        }

        if (stageSuccess) {
          successCount += stageJobs.size();
          addMessage("");
        } else {
          for (Job job : stageJobs) {
            if (job.isSuccess()) successCount++;
            else if (job.isFailed()) failedCount++;
          }

          addMessage("");
          addMessage("✗ Stage " + stageName + " FAILED");
          addMessage("Stopping pipeline execution (fail-fast mode)");
          pipelineFailed = true;
          break;
        }
      }

      // ========== 8. Print final status ==========
      if (!pipelineFailed) {
        addMessage("========================================");
        addMessage("✓ Pipeline Completed Successfully");
        addMessage("========================================");
      } else {
        addMessage("========================================");
        addMessage("✗ Pipeline FAILED");
        addMessage("========================================");
      }

    } catch (Exception e) {
      addMessage("ERROR: Pipeline execution failed with exception");
      addMessage("  " + e.getMessage());
      e.printStackTrace();
      pipelineFailed = true;

    } finally {
      // ========== 9. Cleanup and print summary ==========
      scheduler.shutdown();

      int skippedCount = totalJobs - successCount - failedCount;

      addMessage("");
      addMessage("Execution Summary:");
      addMessage("  Total jobs:    " + totalJobs);
      addMessage("  Succeeded:     " + successCount);
      addMessage("  Failed:        " + failedCount);
      addMessage("  Skipped:       " + skippedCount);

      // ===== datastore: run end =====
      OffsetDateTime pipelineEnd = OffsetDateTime.now();
      ds.finishRun(pipeline, runNoNum, pipelineEnd, pipelineFailed ? "failed" : "success");

      setWorkDone();
    }
  }
}
