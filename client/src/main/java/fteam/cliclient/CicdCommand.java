package fteam.cliclient;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Entry point for the CLI client.
 *
 * <p>This command registers the available subcommands and delegates execution to picocli.
 */
@Command(
    name = "cicd",
    description =
        "Command-line client for validating, previewing, running,"
            + " and reporting CI/CD pipelines.",
    version = "0.1.0",
    subcommands = {
      VerifySubcommand.class,
      DryrunSubcommand.class,
      RunSubcommand.class,
      ReportSubcommand.class
    })
public class CicdCommand implements Callable<Integer> {

  @Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "Show command help and exit.")
  boolean helpRequested;

  @Option(
      names = {"-V", "--version"},
      versionHelp = true,
      description = "Show version information and exit.")
  boolean versionRequested;

  /** Creates the root CLI command. */
  public CicdCommand() {}

  /**
   * Starts the CLI application.
   *
   * @param args command-line arguments provided by the user
   */
  public static void main(String[] args) {
    int exitcode = new CommandLine(new CicdCommand()).execute(args);
    System.exit(exitcode);
  }

  /**
   * Prints the top-level usage message when no subcommand is provided.
   *
   * @return exit code {@code 0}
   */
  @Override
  public Integer call() {
    CommandLine.usage(this, System.out);
    return 0;
  }
}
