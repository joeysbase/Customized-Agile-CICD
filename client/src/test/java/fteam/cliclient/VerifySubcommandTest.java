package fteam.cliclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fteam.testutil.ClientPipelineFixtures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** Tests file and directory validation behavior for the verify subcommand. */
class VerifySubcommandTest extends SubcommandTestBase {

  @TempDir
  Path tempDir;

  private int run(String... args) {
    return new CommandLine(new VerifySubcommand()).execute(args);
  }

  @Test
  void noArgsFallsBackToDefaultAndReturnsOne() {
    assertEquals(1, run());
    assertTrue(stdout().contains("Warning"));
  }

  @Test
  void nonexistentPathReturnsOne() {
    assertEquals(1, run("/no/such/file.yaml"));
    assertTrue(stderr().contains("does not exist"));
  }

  @Test
  void singleYamlFileReturnsZero() throws IOException {
    Path yamlFile = tempDir.resolve("pipeline.yaml");
    Files.writeString(yamlFile, ClientPipelineFixtures.VALID_SINGLE_JOB);
    assertEquals(0, run(yamlFile.toString()));
    assertTrue(stdout().contains("Verification results"));
  }

  @Test
  void directoryWithYamlFilesProcessesEachYaml() throws IOException {
    Files.writeString(tempDir.resolve("a.yaml"), ClientPipelineFixtures.VALID_SINGLE_JOB);
    Files.writeString(tempDir.resolve("b.yml"), ClientPipelineFixtures.VALID_WITH_NEEDS);

    assertEquals(0, run(tempDir.toString()));
    String out = stdout();
    assertTrue(out.contains("a.yaml"));
    assertTrue(out.contains("b.yml"));
  }

  @Test
  void directoryWithMixedFilesIgnoresNonYamlFiles() throws IOException {
    Files.writeString(tempDir.resolve("good.yaml"), ClientPipelineFixtures.VALID_SINGLE_JOB);
    Files.writeString(tempDir.resolve("ignore.txt"), "not yaml");

    assertEquals(0, run(tempDir.toString()));
    assertTrue(stdout().contains("good.yaml"));
    assertFalse(stdout().contains("ignore.txt"));
  }
}
