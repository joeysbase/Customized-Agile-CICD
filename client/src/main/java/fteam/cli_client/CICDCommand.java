package fteam.cli_client;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "cicd",
    description = "A cicd cli tool",
    version = "0.1.0",
    mixinStandardHelpOptions = true,
    subcommands = {VerifySubcommand.class, DryrunSubcommand.class, RunSubcommand.class, ReportSubcommand.class})
public class CICDCommand implements Callable<Integer> {
  public static void main(String[] args) {
    int exitcode = new CommandLine(new CICDCommand()).execute(args);
    System.exit(exitcode);
  }

  @Override
  public Integer call() {
    return 0;
  }
}
