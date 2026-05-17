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

/** Tests validation and happy-path dispatch for the dryrun subcommand. */
class DryrunSubcommandTest extends SubcommandTestBase {

  @TempDir
  Path tempDir;

  private int run(String... args) {
    return new CommandLine(new DryrunSubcommand()).execute(args);
  }

  @Test
  void noArgsReturnsOne() {
    assertEquals(1, run());
    assertTrue(stderr().contains("required"));
  }

  @Test
  void nonexistentFileReturnsOne() {
    assertEquals(1, run("/no/such/file.yaml"));
    assertTrue(stderr().contains("does not exist"));
  }

  @Test
  void directoryPathReturnsOne() {
    assertEquals(1, run(tempDir.toString()));
    assertTrue(stderr().contains("directory"));
  }

  @Test
  void validYamlFileReturnsZeroAndPrintsHeader() throws IOException {
    Path yamlFile = tempDir.resolve("pipeline.yaml");
    Files.writeString(yamlFile, ClientPipelineFixtures.VALID_SINGLE_JOB);
    assertEquals(0, run(yamlFile.toString()));
    assertTrue(stdout().contains("Dry-run results"));
  }
}
