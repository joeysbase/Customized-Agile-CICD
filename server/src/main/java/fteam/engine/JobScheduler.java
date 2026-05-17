package fteam.engine;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Job scheduler responsible for scheduling and executing jobs based on dependencies. */
public class JobScheduler {
  private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);
  private final ExecutorService executorService;
  private final File repoDir;
  private final String pvcName;
  private final Job.JobExecutionCallback callback;

  /**
   * Creates a scheduler with a default cached thread pool.
   *
   * @param repoDir working directory for job execution
   * @param callback callback for logging job output
   */
  public JobScheduler(File repoDir, Job.JobExecutionCallback callback) {
    this(repoDir, null, callback);
  }

  /**
   * Creates a scheduler that can also pass a Kubernetes PVC workspace to jobs.
   *
   * @param repoDir working directory for job execution
   * @param pvcName workspace PVC name for Kubernetes execution, or {@code null}
   * @param callback callback for log output
   */
  public JobScheduler(File repoDir, String pvcName, Job.JobExecutionCallback callback) {
    this.repoDir = repoDir;
    this.pvcName = pvcName;
    this.callback = callback;
    this.executorService =
        Executors.newCachedThreadPool(
            r -> {
              Thread t = new Thread(r);
              t.setName("JobThread-" + t.getId());
              t.setDaemon(false);
              return t;
            });
  }

  /**
   * Creates a scheduler with a fixed thread pool size.
   *
   * @param threadPoolSize maximum number of concurrent threads
   * @param repoDir working directory for job execution
   * @param callback callback for logging job output
   */
  public JobScheduler(int threadPoolSize, File repoDir, Job.JobExecutionCallback callback) {
    this.repoDir = repoDir;
    this.pvcName = null;
    this.callback = callback;
    this.executorService =
        Executors.newFixedThreadPool(
            threadPoolSize,
            r -> {
              Thread t = new Thread(r);
              t.setName("JobThread-" + t.getId());
              t.setDaemon(false);
              return t;
            });
  }

  /**
   * Executes all jobs in a stage.
   *
   * @param stageName name of the stage
   * @param jobs all jobs in this stage
   * @return {@code true} if all jobs succeeded, {@code false} otherwise
   */
  public boolean executeStage(String stageName, List<Job> jobs) {
    if (jobs == null || jobs.isEmpty()) {
      return true;
    }

    Tracer tracer = GlobalOpenTelemetry.getTracer("cicd-server");
    Span stageSpan = tracer.spanBuilder("stage:" + stageName).startSpan();
    callback.onMessage("\n========== Executing Stage: " + stageName + " ==========");

    try (Scope stageScope = stageSpan.makeCurrent()) {
      Map<String, Job> jobMap = jobs.stream().collect(Collectors.toMap(Job::getName, j -> j));

      List<List<Job>> executionLevels = topologicalSort(jobs, jobMap);

      callback.onMessage("Total levels: " + executionLevels.size());

      for (int level = 0; level < executionLevels.size(); level++) {
        List<Job> currentLevel = executionLevels.get(level);

        String levelInfo =
            "\n--- Level "
                + level
                + ": "
                + currentLevel.stream().map(Job::getName).collect(Collectors.joining(", "))
                + " ---";
        callback.onMessage(levelInfo);

        boolean levelSuccess = executeLevel(currentLevel);

        if (!levelSuccess) {
          callback.onMessage("✗ Stage " + stageName + " failed at level " + level);
          stageSpan.setStatus(StatusCode.ERROR, "Stage failed at level " + level);
          return false;
        }
      }

      callback.onMessage("\n✓ Stage " + stageName + " completed successfully");
      stageSpan.setStatus(StatusCode.OK);
      return true;

    } catch (Exception e) {
      callback.onMessage("✗ Stage " + stageName + " failed with exception: " + e.getMessage());
      logger.error("Stage {} failed with exception", stageName, e);
      stageSpan.setStatus(StatusCode.ERROR, e.getMessage());
      return false;
    } finally {
      stageSpan.end();
    }
  }

  /** Executes one level of jobs in parallel within the level. */
  private boolean executeLevel(List<Job> jobs) {
    for (Job job : jobs) {
      job.setRepoDir(repoDir);
      job.setPvcName(pvcName);
      job.setCallback(callback);
    }

    Map<String, String> parentMdc = MDC.getCopyOfContextMap();
    Context parentContext = Context.current();

    List<Future<?>> futures = new ArrayList<>();
    for (Job job : jobs) {
      Future<?> future =
          executorService.submit(
              () -> {
                if (parentMdc != null) {
                  MDC.setContextMap(parentMdc);
                }
                try (Scope ignored = parentContext.makeCurrent()) {
                  job.run();
                } finally {
                  MDC.clear();
                }
              });
      futures.add(future);
    }

    boolean allSuccess = true;

    for (int i = 0; i < jobs.size(); i++) {
      Job job = jobs.get(i);
      try {
        futures.get(i).get();

        if (job.isSuccess()) {
          callback.onMessage("  ✓ " + job.getName() + " succeeded");
        } else if (job.isFailed() && job.isFailureAllowed()) {
          callback.onMessage(
              "  ⚠ " + job.getName() + " failed (allowed): " + job.getErrorMessage());
        } else {
          callback.onMessage("  ✗ " + job.getName() + " failed: " + job.getErrorMessage());
          allSuccess = false;
        }

      } catch (InterruptedException e) {
        callback.onMessage("  ✗ " + job.getName() + " interrupted: " + e.getMessage());
        allSuccess = false;
        Thread.currentThread().interrupt();

      } catch (ExecutionException e) {
        callback.onMessage("  ✗ " + job.getName() + " execution error: " + e.getMessage());
        allSuccess = false;
      }
    }

    return allSuccess;
  }

  /**
   * Groups jobs by dependency levels using a topological sort.
   *
   * <p>Level 0 contains jobs with no dependencies. Level 1 contains jobs that only depend on
   * Level 0. Level N contains jobs that only depend on Levels 0..N-1.
   */
  private List<List<Job>> topologicalSort(List<Job> jobs, Map<String, Job> jobMap) {
    List<List<Job>> levels = new ArrayList<>();
    Set<String> completed = new HashSet<>();

    int maxIterations = jobs.size() + 1; // Prevent infinite loop
    int iteration = 0;

    while (completed.size() < jobs.size()) {
      int currentIteration = iteration;
      iteration++;
      if (currentIteration > maxIterations) {
        throw new IllegalStateException(
            "Circular dependency detected in jobs: "
                + jobs.stream()
                    .filter(j -> !completed.contains(j.getName()))
                    .map(Job::getName)
                    .collect(Collectors.joining(", ")));
      }

      List<Job> currentLevel = new ArrayList<>();

      for (Job job : jobs) {
        if (completed.contains(job.getName())) {
          continue;
        }

        // Check if all dependencies are satisfied
        List<String> needs = job.getNeeds();
        if (needs == null || needs.isEmpty()) {
          // No dependencies, can execute
          currentLevel.add(job);
        } else if (completed.containsAll(needs)) {
          // All dependencies completed, can execute
          currentLevel.add(job);
        }
      }

      if (currentLevel.isEmpty()) {
        // No jobs can execute, but some jobs remain = circular dependency
        List<String> remaining =
            jobs.stream()
                .filter(j -> !completed.contains(j.getName()))
                .map(Job::getName)
                .collect(Collectors.toList());
        throw new IllegalStateException("Circular dependency detected among jobs: " + remaining);
      }

      levels.add(currentLevel);
      currentLevel.forEach(j -> completed.add(j.getName()));
    }

    return levels;
  }

  /** Shuts down the scheduler gracefully. */
  public void shutdown() {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        callback.onMessage("ExecutorService did not terminate in time, forcing shutdown...");
        executorService.shutdownNow();

        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
          callback.onMessage("ExecutorService did not terminate after force shutdown");
        }
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
