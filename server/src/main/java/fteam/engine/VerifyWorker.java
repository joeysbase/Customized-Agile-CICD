package fteam.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Worker that validates a pipeline configuration and publishes validation messages.
 */
public class VerifyWorker extends Worker {
  private final String configurationString;

  private VerifyWorker(String configStr) {
    this.configurationString = configStr;
  }

  /**
   * Creates a verify worker from a file on disk.
   *
   * @param path path to the configuration file
   * @return worker instance or {@code null} when the file cannot be read
   */
  public static VerifyWorker fromFile(String path) {
    Path filePath = Path.of(path);
    try {
      String content = Files.readString(filePath);
      return new VerifyWorker(content);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Creates a verify worker from inline configuration content.
   *
   * @param fileString configuration content
   * @return worker instance
   */
  public static VerifyWorker fromFileString(String fileString) {
    return new VerifyWorker(fileString);
  }

  /**
   * Validates the configuration and appends all validation messages to the worker output.
   */
  @Override
  public void run() {
    PipelineConfig config = PipelineConfig.fromFile(configurationString);
    for (String s : config.getVerificationMsg()) {
      addMessage(s);
    }
    setWorkDone();
  }
}
