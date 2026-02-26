package fteam.engine;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Job Scheduler: Responsible for scheduling and executing jobs based on dependencies
 */
public class JobScheduler {
  private final ExecutorService executorService;
  private final File repoDir;
  private final Job.JobExecutionCallback callback;

  /**
   * Constructor with default cached thread pool
   *
   * @param repoDir Working directory for job execution
   * @param callback Callback for logging job output
   */
  public JobScheduler(File repoDir, Job.JobExecutionCallback callback) {
    this.repoDir = repoDir;
    this.callback = callback;
    this.executorService = Executors.newCachedThreadPool(r -> {
      Thread t = new Thread(r);
      t.setName("JobThread-" + t.getId());
      t.setDaemon(false);
      return t;
    });
  }

  /**
   * Constructor with fixed thread pool size
   *
   * @param threadPoolSize Maximum number of concurrent threads
   * @param repoDir Working directory for job execution
   * @param callback Callback for logging job output
   */
  public JobScheduler(int threadPoolSize, File repoDir, Job.JobExecutionCallback callback) {
    this.repoDir = repoDir;
    this.callback = callback;
    this.executorService = Executors.newFixedThreadPool(threadPoolSize, r -> {
      Thread t = new Thread(r);
      t.setName("JobThread-" + t.getId());
      t.setDaemon(false);
      return t;
    });
  }

  /**
   * Execute all jobs in a stage
   *
   * @param stageName Name of the stage
   * @param jobs All jobs in this stage
   * @return true if all jobs succeeded, false otherwise
   */
  public boolean executeStage(String stageName, List<Job> jobs) {
    if (jobs == null || jobs.isEmpty()) {
      return true;
    }

    callback.onMessage("\n========== Executing Stage: " + stageName + " ==========");

    try {
      // 1. Build dependency graph
      Map<String, Job> jobMap = jobs.stream()
          .collect(Collectors.toMap(Job::getName, j -> j));

      // 2. Topological sort for level-based execution
      List<List<Job>> executionLevels = topologicalSort(jobs, jobMap);

      callback.onMessage("Total levels: " + executionLevels.size());

      // 3. Execute level by level
      for (int level = 0; level < executionLevels.size(); level++) {
        List<Job> currentLevel = executionLevels.get(level);

        String levelInfo = "\n--- Level " + level + ": " +
            currentLevel.stream().map(Job::getName).collect(Collectors.joining(", ")) + " ---";
        callback.onMessage(levelInfo);

        boolean levelSuccess = executeLevel(currentLevel);

        if (!levelSuccess) {
          callback.onMessage("✗ Stage " + stageName + " failed at level " + level);
          return false;
        }
      }

      callback.onMessage("\n✓ Stage " + stageName + " completed successfully");
      return true;

    } catch (Exception e) {
      callback.onMessage("✗ Stage " + stageName + " failed with exception: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Execute one level of jobs (parallel within the level)
   */
  private boolean executeLevel(List<Job> jobs) {
    // Set execution context for all jobs in this level
    for (Job job : jobs) {
      job.setRepoDir(repoDir);
      job.setCallback(callback);
    }

    // Submit all jobs to thread pool
    List<Future<?>> futures = new ArrayList<>();
    for (Job job : jobs) {
      Future<?> future = executorService.submit(job);
      futures.add(future);
    }

    // Wait for all jobs to complete
    boolean allSuccess = true;
    for (int i = 0; i < jobs.size(); i++) {
      Job job = jobs.get(i);
      try {
        futures.get(i).get(); // Block until job completes

        if (job.isSuccess()) {
          callback.onMessage("  ✓ " + job.getName() + " succeeded");
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
   * Topological sort: Group jobs by dependency levels
   *
   * Level 0: Jobs with no dependencies
   * Level 1: Jobs that only depend on Level 0
   * Level N: Jobs that only depend on Levels 0..N-1
   */
  private List<List<Job>> topologicalSort(List<Job> jobs, Map<String, Job> jobMap) {
    List<List<Job>> levels = new ArrayList<>();
    Set<String> completed = new HashSet<>();

    int maxIterations = jobs.size() + 1; // Prevent infinite loop
    int iteration = 0;

    while (completed.size() < jobs.size()) {
      if (iteration++ > maxIterations) {
        throw new IllegalStateException("Circular dependency detected in jobs: " +
            jobs.stream()
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
        List<String> remaining = jobs.stream()
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

  /**
   * Shutdown the scheduler gracefully
   */
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