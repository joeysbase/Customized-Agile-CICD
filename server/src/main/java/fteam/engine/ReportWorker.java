package fteam.engine;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReportWorker extends Worker {

  private final String pipelineName;
  private final String runNo;
  private final String stageName;
  private final String jobName;

  private List<String> messages;
  private AtomicBoolean status;

  public ReportWorker(String pipelineName, String runNo, String stageName, String jobName) {
    this.pipelineName = pipelineName;
    this.runNo = runNo;
    this.stageName = stageName;
    this.jobName = jobName;
  }

  @Override
  public void setMessages(List<String> messages) {
    this.messages = messages;
  }

  @Override
  public void setStatus(AtomicBoolean status) {
    this.status = status;
  }

  @Override
  public void run() {
    try {
      if (messages == null) {
        throw new IllegalStateException("messages not set");
      }
      if (status == null) {
        throw new IllegalStateException("status not set");
      }

      if (pipelineName == null || pipelineName.isBlank()) {
        messages.add("Error: --pipeline is required");
        return;
      }
      if (stageName != null && (runNo == null || runNo.isBlank())) {
        messages.add("Error: --stage requires --run");
        return;
      }
      if (jobName != null && (stageName == null || stageName.isBlank())) {
        messages.add("Error: --job requires --stage");
        return;
      }

      String reportText = ReportService.renderReport(pipelineName, emptyToNull(runNo), emptyToNull(stageName), emptyToNull(jobName));
      messages.add(reportText);

    } catch (Exception e) {
      if (messages != null) messages.add("Server error: " + e.getMessage());
    } finally {
      if (status != null) status.set(true);
    }
  }

  private static String emptyToNull(String s) {
    return (s == null || s.isBlank()) ? null : s;
  }
}