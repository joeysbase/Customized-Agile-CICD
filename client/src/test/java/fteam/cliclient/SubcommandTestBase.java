package fteam.cliclient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/** Shared stdout/stderr capture for CLI subcommand tests. */
abstract class SubcommandTestBase {

  protected ByteArrayOutputStream outBuf;
  protected ByteArrayOutputStream errBuf;

  private PrintStream savedOut;
  private PrintStream savedErr;

  @BeforeEach
  void captureOutput() {
    savedOut = System.out;
    savedErr = System.err;
    outBuf = new ByteArrayOutputStream();
    errBuf = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outBuf));
    System.setErr(new PrintStream(errBuf));
  }

  @AfterEach
  void restoreOutput() {
    System.setOut(savedOut);
    System.setErr(savedErr);
  }

  protected String stdout() {
    return outBuf.toString();
  }

  protected String stderr() {
    return errBuf.toString();
  }
}
