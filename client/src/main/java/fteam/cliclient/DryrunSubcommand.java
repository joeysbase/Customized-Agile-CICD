package fteam.cliclient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/** CLI subcommand that validates a pipeline file and prints the planned execution order. */
@Command(
    name = "dryrun",
    description = "Validate a pipeline file and print the planned job execution order.",
    mixinStandardHelpOptions = true)
public class DryrunSubcommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      description = "Path to the pipeline configuration file to preview.",
      defaultValue = "")
  String filePath;

  /** Creates the dry-run CLI subcommand. */
  public DryrunSubcommand() {}

  /**
   * Executes the dry-run request for the provided pipeline file.
   *
   * @return exit code {@code 0} on success or {@code 1} when the file is invalid
   */
  @Override
  public Integer call() {
    if (filePath.isEmpty()) {
      System.err.println(
          "Error: A pipeline configuration file is required. "
              + "Use `cicd dryrun --help` to see usage details.");
      return 1;
    }

    Path path = Path.of(filePath).toAbsolutePath().normalize();

    if (!Files.exists(path)) {
      System.err.println("Error: The file \"" + path + "\" does not exist.");
      return 1;
    }

    if (Files.isDirectory(path)) {
      System.err.println("Error: \"" + path + "\" is a directory. Please provide a file path.");
      return 1;
    }

    String yamlContent;
    try {
      yamlContent = Files.readString(path);
    } catch (Exception e) {
      System.err.println("Error: Cannot read file \"" + path + "\": " + e.getMessage());
      return 1;
    }

    RequestAgent.MESSAGES.clear();
    RequestAgent.postDryrunWorker(yamlContent);

    System.out.println("Dry-run results for " + path + " are as follows:");
    for (String s : RequestAgent.MESSAGES) {
      System.out.println(s);
    }

    return 0;
  }
}
