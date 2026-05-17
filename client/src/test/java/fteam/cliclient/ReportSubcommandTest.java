package fteam.cliclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/** Tests validation rules for report command option combinations. */
class ReportSubcommandTest extends SubcommandTestBase {

  private static final String PIPELINE_FLAG = "--pipeline";
  private static final String PIPELINE_NAME = "my-pipe";

  private int run(String... args) {
    return new CommandLine(new ReportSubcommand()).execute(args);
  }

  @Test
  void noPipelineReturnsOne() {
    assertEquals(1, run());
    assertTrue(stderr().contains(PIPELINE_FLAG));
  }

  @Test
  void stageWithoutRunReturnsOne() {
    assertEquals(1, run(PIPELINE_FLAG, PIPELINE_NAME, "--stage", "build"));
  }

  @Test
  void jobWithoutStageReturnsOne() {
    assertEquals(1, run(PIPELINE_FLAG, PIPELINE_NAME, "--run", "1", "--job", "compile"));
  }

  @Test
  void nonIntegerRunReturnsOne() {
    assertEquals(1, run(PIPELINE_FLAG, PIPELINE_NAME, "--run", "abc"));
  }

  @Test
  void validPipelineOnlyRequestReturnsZero() {
    assertEquals(0, run(PIPELINE_FLAG, "my-pipeline"));
  }
}
