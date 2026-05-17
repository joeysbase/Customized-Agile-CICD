package fteam.engine;

import fteam.engine.git.GitAgent;
import fteam.engine.git.GitUtil;
import fteam.engine.git.LocalGitAgent;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Creates and tracks asynchronous workers for verify, dry-run, run, and report requests. */
public class WorkerManager {
  private final Map<Integer, WorkerMonitor> workers = new ConcurrentHashMap<>();
  private final ExecutorService warmPool = Executors.newFixedThreadPool(64);

  /** Creates a worker manager with a shared execution pool. */
  public WorkerManager() {}

  /** Mutable monitor shared with a single worker execution. */
  public class WorkerMonitor {
    /** Indicates whether the associated worker has completed. */
    public final AtomicBoolean status = new AtomicBoolean(false);

    /** Collects status and log messages produced by the worker. */
    public final List<String> messages = new CopyOnWriteArrayList<>();

    /** Creates an empty monitor for a new worker execution. */
    public WorkerMonitor() {}
  }

  /**
   * Starts a verification worker.
   *
   * @param content pipeline configuration content
   * @return worker identifier
   */
  public int getVerifyWorker(String content) {
    Worker verifyWorker = VerifyWorker.fromFileString(content);
    WorkerMonitor monitor = new WorkerMonitor();
    verifyWorker.setMessages(monitor.messages);
    verifyWorker.setStatus(monitor.status);
    Integer id = (verifyWorker.toString() + Instant.now().toString()).hashCode();
    workers.put(id, monitor);
    warmPool.submit(verifyWorker);
    return id;
  }

  /**
   * Starts a dry-run worker.
   *
   * @param content pipeline configuration content
   * @return worker identifier
   */
  public int getDryrunWorker(String content) {
    Worker dryrunWorker = DryrunWorker.fromFileString(content);
    WorkerMonitor monitor = new WorkerMonitor();
    dryrunWorker.setMessages(monitor.messages);
    dryrunWorker.setStatus(monitor.status);

    Integer id = (dryrunWorker.toString() + java.time.Instant.now()).hashCode();
    workers.put(id, monitor);
    warmPool.submit(dryrunWorker);
    return id;
  }

  /**
   * Starts a pipeline run worker after validating the requested Git state.
   *
   * @param content pipeline configuration content
   * @param repo repository path
   * @param commit requested commit
   * @param branch requested branch
   * @return worker identifier
   */
  public int getRunWorker(String content, String repo, String commit, String branch) {
    Worker runWorker;
    WorkerMonitor monitor = new WorkerMonitor();

    try {
      if (KubernetesJobExecutor.isInCluster()) {
        // k8s mode: skip git check, no local repo needed
        runWorker = RunWorker.fromFileString(content, null);
      } else {
        // Local mode: git check + prepare workspace
        File repoDir = new File(repo);

        String currentBranch = GitUtil.currentBranch(repoDir);
        String currentCommit = GitUtil.currentCommit(repoDir);

        if (branch != null && !branch.isEmpty() && !branch.equals(currentBranch)) {
          monitor.messages.add(
              "ERROR: requested branch "
                  + branch
                  + " but current branch is "
                  + currentBranch
                  + ". Please checkout "
                  + branch
                  + " first.");
          monitor.status.set(true);
          Integer id = ("failed-run-" + java.time.Instant.now()).hashCode();
          workers.put(id, monitor);
          return id;
        }

        if (commit != null
            && !commit.isEmpty()
            && !"latest".equals(commit)
            && !commit.equals(currentCommit)) {
          monitor.messages.add(
              "ERROR: requested commit "
                  + commit
                  + " but current HEAD is "
                  + currentCommit
                  + ". Please checkout the requested commit first.");
          monitor.status.set(true);
          Integer id = ("failed-run-" + java.time.Instant.now()).hashCode();
          workers.put(id, monitor);
          return id;
        }

        GitAgent git = new LocalGitAgent();
        File workspace = git.prepareWorkspace(repo, branch, commit);

        runWorker = RunWorker.fromFileString(content, workspace);
      }

    } catch (Exception e) {
      monitor.messages.add("ERROR: " + e.getMessage());
      monitor.status.set(true);
      Integer id = ("failed-run-" + java.time.Instant.now()).hashCode();
      workers.put(id, monitor);
      return id;
    }

    ((RunWorker) runWorker).setGitInfo(branch, commit);
    runWorker.setMessages(monitor.messages);
    runWorker.setStatus(monitor.status);

    Integer id = (runWorker.toString() + java.time.Instant.now()).hashCode();
    workers.put(id, monitor);
    warmPool.submit(runWorker);
    return id;
  }

  /**
   * Starts a report worker.
   *
   * @param pipelineName pipeline name
   * @param runNo optional run number
   * @param stageName optional stage name
   * @param jobName optional job name
   * @return worker identifier
   */
  public int getReportWorker(String pipelineName, String runNo, String stageName, String jobName) {
    Worker reportWorker = new ReportWorker(pipelineName, runNo, stageName, jobName);
    WorkerMonitor monitor = new WorkerMonitor();

    reportWorker.setMessages(monitor.messages);
    reportWorker.setStatus(monitor.status);

    Integer id = (reportWorker.toString() + java.time.Instant.now()).hashCode();
    workers.put(id, monitor);
    warmPool.submit(reportWorker);
    return id;
  }

  /**
   * Returns the completion state of the identified worker.
   *
   * @param id worker identifier
   * @return {@code true} when the worker has finished
   */
  public boolean getWorkerStatus(int id) {
    boolean isDone = workers.get(id).status.get();
    return isDone;
  }

  /**
   * Returns all messages collected for the identified worker.
   *
   * @param id worker identifier
   * @return worker messages
   */
  public List<String> getWorkerMessages(int id) {
    return workers.get(id).messages;
  }

  /**
   * Removes the identified worker monitor from the manager.
   *
   * @param id worker identifier
   */
  public void removeWorker(int id) {
    workers.remove(id);
  }
}
