package fteam.cliclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fteam.testutil.ClientPipelineFixtures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** Tests argument validation for the run subcommand. */
class RunSubcommandTest extends SubcommandTestBase {

  @TempDir
  Path tempDir;

  private int run(String... args) {
    return new CommandLine(new RunSubcommand()).execute(args);
  }

  @Test
  void noFileAndNoNameReturnsOne() {
    assertEquals(1, run());
    assertTrue(
        stderr().contains("pipeline file is required")
            || stderr().contains("pipeline file"));
  }

  @Test
  void bothFileAndNameReturnsOne() throws IOException {
    Path yamlFile = tempDir.resolve("pipe.yaml");
    Files.writeString(yamlFile, ClientPipelineFixtures.VALID_SINGLE_JOB);
    assertEquals(1, run("--file", yamlFile.toString(), "--name", "pipe"));
  }

  @Test
  void nonexistentNamedPipelineReturnsOne() {
    assertEquals(1, run("--name", "nonexistent-pipeline"));
  }

  @Test
  void validFileReturnsZero() throws IOException {
    Path yamlFile = tempDir.resolve("pipeline.yaml");
    Files.writeString(yamlFile, ClientPipelineFixtures.VALID_SINGLE_JOB);
    assertEquals(0, run("--file", yamlFile.toString(), "--repo", tempDir.toString()));
  }
}
