package fteam.cli_client;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "report",
    description = "Get reports of pipeline runs",
    mixinStandardHelpOptions = true)
public class ReportSubcommand implements Callable<Integer> {

  @Option(
      names = {"--pipeline"},
      description = "Specify the name of a pipeline of which reports are requested",
      defaultValue = "",
      paramLabel = "PIPELINE_NAME")
  String pipelineName;

  @Option(
      names = {"--run"},
      description = "Specify the id of a specific a pipeline run",
      defaultValue = "",
      paramLabel = "RUN_NUMBER")
  String runNo;

  @Option(
      names = {"--stage"},
      description = "Specify a stage of a pipeline run",
      defaultValue = "",
      paramLabel = "STAGE_NAME")
  String stage;

  @Option(
      names = {"--job"},
      description = "Specify a job of a stage of a pipeline run",
      defaultValue = "",
      paramLabel = "JOB_NAME")
  String jobName;

  @Override
  public Integer call() {
    if (pipelineName == null || pipelineName.isBlank()) {
      System.err.println("Error: --pipeline option cannot be empty. You must give one.");
      return 1;
    }

    // ✅ normalize: "" -> null
    String run = (runNo == null || runNo.isBlank()) ? null : runNo;
    String st = (stage == null || stage.isBlank()) ? null : stage;
    String job = (jobName == null || jobName.isBlank()) ? null : jobName;

    // ✅ spec dependency validation
    if (st != null && run == null) {
      System.err.println("Error: --stage requires --run");
      return 1;
    }
    if (job != null && st == null) {
      System.err.println("Error: --job requires --stage");
      return 1;
    }

    // ✅ run must be integer (when provided)
    if (run != null) {
      try {
        Integer.parseInt(run);
      } catch (NumberFormatException e) {
        System.err.println("Error: --run must be an integer");
        return 1;
      }
    }

    // ✅ IMPORTANT: pass normalized values
    RequestAgent.requestReportWorker(pipelineName, run, st, job);

    for (String s : RequestAgent.MESSAGES) {
      System.out.println(s);
    }
    return 0;
  }
}