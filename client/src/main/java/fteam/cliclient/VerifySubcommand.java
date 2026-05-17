package fteam.cliclient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/** CLI subcommand that validates one pipeline file or every YAML file inside a directory. */
@Command(
    name = "verify",
    description = "Validate one pipeline file or every YAML pipeline file in a directory.",
    mixinStandardHelpOptions = true)
public class VerifySubcommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      description = "Path to a pipeline configuration file or a directory containing YAML files.",
      defaultValue = "",
      paramLabel = "FILE")
  String filePath;

  /** Creates the verify CLI subcommand. */
  public VerifySubcommand() {}

  /**
   * Validates the requested file or directory and prints the validation results.
   *
   * @return exit code {@code 0} on success or {@code 1} when the path is invalid
   */
  @Override
  public Integer call() {
    List<String> results = new ArrayList<>();

    if (filePath.isEmpty()) {
      System.out.println(
          "Warning: No configuration file or directory was provided, "
              + "using default value \".pipelines/default.yaml\".");
      filePath = ".pipelines/default.yaml";
    }

    Path path = Path.of(filePath).toAbsolutePath().normalize();

    if (Files.isDirectory(path)) {
      File dir = path.toFile();
      File[] files = dir.listFiles();

      if (files == null) {
        System.err.println("Error: Unable to read directory \"" + path + "\".");
        return 1;
      }

      for (File file : files) {
        if (file.isFile()
            && (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml"))) {

          StringBuilder sb = new StringBuilder();
          sb.append(file.getPath()).append(":\n");

          try {
            System.out.println("Verifying " + file.getName() + "...\n");

            String yamlContent = Files.readString(file.toPath());
            RequestAgent.MESSAGES.clear();
            RequestAgent.postVerifyWorker(yamlContent);

            for (String msg : RequestAgent.MESSAGES) {
              sb.append("\t- ").append(msg).append("\n");
            }
            results.add(sb.toString());

          } catch (Exception e) {
            sb.append("\t- Error: ").append(e.getMessage()).append("\n");
            results.add(sb.toString());
          }
        }
      }

    } else if (Files.isRegularFile(path)) {
      File file = path.toFile();
      StringBuilder sb = new StringBuilder();
      sb.append(file.getPath()).append(":\n");

      try {
        System.out.println("Verifying " + file.getName() + "...\n");

        String yamlContent = Files.readString(path);
        RequestAgent.MESSAGES.clear();
        RequestAgent.postVerifyWorker(yamlContent);

        for (String msg : RequestAgent.MESSAGES) {
          sb.append("\t- ").append(msg).append("\n");
        }
        results.add(sb.toString());

      } catch (Exception e) {
        sb.append("\t- Error: ").append(e.getMessage()).append("\n");
        results.add(sb.toString());
      }

    } else {
      System.err.println("Error: The file or directory \"" + path + "\" does not exist.");
      return 1;
    }

    System.out.println("Verification results are as follows:\n");
    for (String s : results) {
      System.out.println(s);
    }

    return 0;
  }
}
