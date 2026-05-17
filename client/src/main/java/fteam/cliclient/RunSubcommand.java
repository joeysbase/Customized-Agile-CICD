package fteam.cliclient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** CLI subcommand that starts a pipeline run against a repository snapshot. */
@Command(
    name = "run",
    description = "Run a pipeline file against a selected repository snapshot.",
    mixinStandardHelpOptions = true)
public class RunSubcommand implements Callable<Integer> {

  @Option(
      names = {"--name"},
      description =
          "Name of a pipeline file under the .pipelines/ directory,"
              + " without the .yaml suffix.",
      defaultValue = "",
      paramLabel = "NAME")
  String name;

  @Option(
      names = {"--file"},
      description = "Path to a pipeline configuration file.",
      defaultValue = "",
      paramLabel = "FILE")
  String file;

  @Option(
      names = {"--commit"},
      description = "Commit hash to run against. Defaults to latest.",
      defaultValue = "latest")
  String commit;

  @Option(
      names = {"--branch"},
      description = "Repository branch to run against. Defaults to main.",
      defaultValue = "main")
  String branch;

  @Option(
      names = {"--repo"},
      description = "Repository path to run against. Defaults to the current working directory.",
      defaultValue = "",
      paramLabel = "REPO")
  String repo;

  /** Creates the run CLI subcommand. */
  public RunSubcommand() {}

  /**
   * Resolves the requested pipeline definition and sends a run request to the server.
   *
   * @return exit code {@code 0} on success or {@code 1} when inputs are invalid
   */
  @Override
  public Integer call() {
    if (name.isEmpty() && file.isEmpty()) {
      System.err.println(
          "Error: A pipeline file is required. Provide either `--name` or `--file`.");
      return 1;
    }

    if (!name.isEmpty() && !file.isEmpty()) {
      System.err.println("Error: Please provide either `--name` or `--file`, but not both.");
      return 1;
    }

    Path pipelineFilePath;
    if (!name.isEmpty()) {
      pipelineFilePath = Path.of(".pipelines", name + ".yaml").toAbsolutePath().normalize();
    } else {
      pipelineFilePath = Path.of(file).toAbsolutePath().normalize();
    }

    if (!Files.exists(pipelineFilePath)) {
      System.err.println(
          "Error: Pipeline file \"" + pipelineFilePath + "\" does not exist or cannot be read.");
      return 1;
    }

    String yamlContent;
    try {
      yamlContent = Files.readString(pipelineFilePath);
    } catch (Exception e) {
      System.err.println(
          "Error: Cannot read pipeline file \""
              + pipelineFilePath
              + "\": "
              + e.getMessage());
      return 1;
    }

    String repoPath = repo;
    if (repoPath == null || repoPath.isBlank()) {
      repoPath = Path.of(".").toAbsolutePath().normalize().toString();
    }

    RequestAgent.MESSAGES.clear();
    RequestAgent.postRunWorker(yamlContent, repoPath, branch, commit);

    for (String s : RequestAgent.MESSAGES) {
      System.out.println(s);
    }

    return 0;
  }
}
