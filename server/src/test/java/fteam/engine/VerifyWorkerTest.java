package fteam.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fteam.testutil.PipelineFixtures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests verify worker happy paths and validation error emission. */
class VerifyWorkerTest {

  private List<String> messages;
  private AtomicBoolean status;

  @BeforeEach
  void setUp() {
    messages = new ArrayList<>();
    status = new AtomicBoolean(false);
  }

  private VerifyWorker workerFor(String yaml) {
    VerifyWorker worker = VerifyWorker.fromFileString(yaml);
    worker.setMessages(messages);
    worker.setStatus(status);
    return worker;
  }

  @Test
  void validYamlProducesNoMessagesAndMarksDone() {
    workerFor(PipelineFixtures.VALID_SINGLE_JOB).run();
    assertTrue(messages.isEmpty());
    assertTrue(status.get());
  }

  @Test
  void invalidYamlProducesMessages() {
    workerFor(PipelineFixtures.INVALID_MISSING_PIPELINE_SECTION).run();
    assertFalse(messages.isEmpty());
  }

  @Test
  void invalidYamlMentionsStageOrCycle() {
    workerFor(PipelineFixtures.INVALID_UNDEFINED_STAGE).run();
    assertTrue(messages.stream().anyMatch(m -> m.contains("nonexistent")));

    messages.clear();
    status.set(false);

    workerFor(PipelineFixtures.INVALID_CYCLE).run();
    assertTrue(messages.stream().anyMatch(m -> m.contains("cycle")));
  }

  @Test
  void fromFileReadsExistingPathAndReturnsNullForMissing(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("pipeline.yaml");
    Files.writeString(file, PipelineFixtures.VALID_SINGLE_JOB);
    assertNotNull(VerifyWorker.fromFile(file.toString()));
    assertNull(VerifyWorker.fromFile("/no/such/file.yaml"));
  }
}
