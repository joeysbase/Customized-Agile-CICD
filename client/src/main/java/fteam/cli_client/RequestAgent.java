package fteam.cli_client;

import java.util.ArrayList;
import java.util.List;

import fteam.engine.WorkerManager;

public class RequestAgent {
  public static final List<String> MESSAGES = new ArrayList<>();

  public static void requestVerifyWorker(String content) {
    WorkerManager manager = new WorkerManager();
    int id = manager.getVerifyWorker(content);
    while (!manager.getWorkerStatus(id)) {}

    setMessages(manager.getWorkerMessages(id));
    manager.removeWorker(id);
  }

  public static void requestDryrunWorker(String content) {
    WorkerManager manager = new WorkerManager();
    int id = manager.getDryrunWorker(content);
    while (!manager.getWorkerStatus(id)) {}

    setMessages(manager.getWorkerMessages(id));
    manager.removeWorker(id);
  }

  public static void requestRunWorker(String content, String branch, String commit) {
    WorkerManager manager = new WorkerManager();
    int id = manager.getRunWorker(content, ".", commit, branch);
    while (!manager.getWorkerStatus(id)) {}

    setMessages(manager.getWorkerMessages(id));
    manager.removeWorker(id);
  }

  public static void requestReportWorker(
      String pipelineName, String runNo, String stageName, String jobName) {
    WorkerManager manager = new WorkerManager();
    int id = manager.getReportWorker(pipelineName, runNo, stageName, jobName);
    while (!manager.getWorkerStatus(id)) {}

    setMessages(manager.getWorkerMessages(id));
    manager.removeWorker(id);
  }

  private static void setMessages(List<String> messages) {
    MESSAGES.addAll(messages);
  }
}
