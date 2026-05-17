package fteam.cliclient;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** CLI subcommand that fetches reports for pipelines, runs, stages, or jobs. */
@Command(
    name = "report",
    description = "Fetch reports for a pipeline, a specific run, a stage, or an individual job.",
    mixinStandardHelpOptions = true)
public class ReportSubcommand implements Callable<Integer> {

  @Option(
      names = {"--pipeline"},
      description = "Pipeline name to report on.",
      defaultValue = "",
      paramLabel = "PIPELINE_NAME")
  String pipelineName;

  @Option(
      names = {"--run"},
      description = "Run number for a specific pipeline execution.",
      defaultValue = "",
      paramLabel = "RUN_NUMBER")
  String runNo;

  @Option(
      names = {"--stage"},
      description = "Stage name within the selected run.",
      defaultValue = "",
      paramLabel = "STAGE_NAME")
  String stage;

  @Option(
      names = {"--job"},
      description = "Job name within the selected stage.",
      defaultValue = "",
      paramLabel = "JOB_NAME")
  String jobName;

  /** Creates the report CLI subcommand. */
  public ReportSubcommand() {}

  /**
   * Sends a report request using the selected pipeline, run, stage, and job filters.
   *
   * @return exit code {@code 0} on success or {@code 1} when the requested option combination is
   *     invalid
   */
  @Override
  public Integer call() {
    if (pipelineName == null || pipelineName.isBlank()) {
      System.err.println("Error: `--pipeline` is required.");
      return 1;
    }

    String run = (runNo == null || runNo.isBlank()) ? null : runNo;
    String st = (stage == null || stage.isBlank()) ? null : stage;
    String job = (jobName == null || jobName.isBlank()) ? null : jobName;

    if (st != null && run == null) {
      System.err.println("Error: `--stage` requires `--run`.");
      return 1;
    }

    if (job != null && st == null) {
      System.err.println("Error: `--job` requires `--stage` (and therefore `--run`).");
      return 1;
    }

    if (run != null) {
      try {
        Integer.parseInt(run);
      } catch (NumberFormatException e) {
        System.err.println("Error: `--run` must be an integer.");
        return 1;
      }
    }

    RequestAgent.MESSAGES.clear();
    RequestAgent.getReportByPath(pipelineName, run, st, job);

    for (String s : RequestAgent.MESSAGES) {
      System.out.println(s);
    }
    return 0;
  }
}
