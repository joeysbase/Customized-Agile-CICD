package fteam.cliclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/** Tests the root CLI command registration and top-level help behavior. */
class CicdCommandTest extends SubcommandTestBase {

  @Test
  void noArgsPrintsUsageAndReturnsZero() {
    int code = new CommandLine(new CicdCommand()).execute();
    assertEquals(0, code);
    assertTrue(stdout().contains("cicd"));
  }

  @Test
  void helpFlagReturnsZero() {
    int code = new CommandLine(new CicdCommand()).execute("--help");
    assertEquals(0, code);
  }

  @Test
  void versionFlagReturnsZero() {
    int code = new CommandLine(new CicdCommand()).execute("--version");
    assertEquals(0, code);
  }

  @Test
  void unknownSubcommandReturnsNonZero() {
    int code = new CommandLine(new CicdCommand()).execute("not-a-real-subcommand");
    assertTrue(code != 0);
  }
}
