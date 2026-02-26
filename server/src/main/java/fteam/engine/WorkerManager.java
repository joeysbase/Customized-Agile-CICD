package fteam.engine;

import fteam.engine.git.GitUtil;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import fteam.engine.git.GitAgent;
import fteam.engine.git.LocalGitAgent;

public class WorkerManager {
  private final Map<Integer, WorkerMonitor> workers = new ConcurrentHashMap<>();
  private final ExecutorService warmPool = Executors.newFixedThreadPool(64);

  public class WorkerMonitor {
    public final AtomicBoolean status = new AtomicBoolean(false);
    public final List<String> messages = new CopyOnWriteArrayList<>();
  }

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

  public int getRunWorker(String content, String repo, String commit, String branch) {
    Worker runWorker;
    WorkerMonitor monitor = new WorkerMonitor();

    try {
      File repoDir = new File(repo);

      String currentBranch = GitUtil.currentBranch(repoDir);
      String currentCommit = GitUtil.currentCommit(repoDir);

      if (branch != null && !branch.isEmpty() && !branch.equals(currentBranch)) {
        monitor.messages.add("ERROR: requested branch " + branch
            + " but current branch is " + currentBranch + ". Please checkout " + branch + " first.");
        monitor.status.set(true);
        Integer id = ("failed-run-" + java.time.Instant.now()).hashCode();
        workers.put(id, monitor);
        return id;
      }

      if (commit != null && !commit.isEmpty() && !"latest".equals(commit) && !commit.equals(currentCommit)) {
        monitor.messages.add("ERROR: requested commit " + commit
            + " but current HEAD is " + currentCommit + ". Please checkout the requested commit first.");
        monitor.status.set(true);
        Integer id = ("failed-run-" + java.time.Instant.now()).hashCode();
        workers.put(id, monitor);
        return id;
      }

      GitAgent git = new LocalGitAgent();
      File workspace = git.prepareWorkspace(repo, branch, commit);

      runWorker = RunWorker.fromFileString(content, workspace);

    } catch (Exception e) {
      monitor.messages.add("ERROR: " + e.getMessage());
      monitor.status.set(true);
      Integer id = ("failed-run-" + java.time.Instant.now()).hashCode();
      workers.put(id, monitor);
      return id;
    }

    runWorker.setMessages(monitor.messages);
    runWorker.setStatus(monitor.status);

    Integer id = (runWorker.toString() + java.time.Instant.now()).hashCode();
    workers.put(id, monitor);
    warmPool.submit(runWorker);
    return id;
  }

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



  public boolean getWorkerStatus(int id) {
    boolean isDone = workers.get(id).status.get();
    return isDone;
  }

  public List<String> getWorkerMessages(int id) {
    return workers.get(id).messages;
  }

  public void removeWorker(int id) {
    workers.remove(id);
  }
}
