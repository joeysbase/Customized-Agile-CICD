package fteam.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DryrunWorker extends Worker {

  private final String configurationString;

  private DryrunWorker(String configStr) {
    this.configurationString = configStr;
  }

  public static DryrunWorker fromFileString(String fileString) {
    return new DryrunWorker(fileString);
  }

  @Override
  public void run() {
    PipelineConfig cfg = PipelineConfig.fromFile(configurationString);

    if (!cfg.isvalidConfig()) {
      for (String s : cfg.getVerificationMsg()) {
        addMessage(s);
      }
      setWorkDone();
      return;
    }

    addMessage("OK: pipeline configuration is valid.");
    addMessage("Dry Run Plan (no commands executed)");
    addMessage("pipeline: " + cfg.getName());

    Map<String, List<Job>> byStage = new LinkedHashMap<>();
    for (String stage : cfg.getStagesInOrder()) {
      byStage.put(stage, new ArrayList<>());
    }
    for (Job j : cfg.getExcutionSequence()) {
      byStage.computeIfAbsent(j.getStage(), k -> new ArrayList<>()).add(j);
    }

    for (Map.Entry<String, List<Job>> e : byStage.entrySet()) {
      String stage = e.getKey();
      List<Job> jobs = e.getValue();
      if (jobs.isEmpty()) continue;

      addMessage("");
      addMessage("Stage: " + stage);
      for (Job j : jobs) {
        String needs = (j.getNeeds() == null || j.getNeeds().isEmpty())
            ? "-"
            : String.join(", ", j.getNeeds());
        addMessage("  - " + j.getName() + " (needs: " + needs + ")");
      }
    }

    setWorkDone();
  }
}
