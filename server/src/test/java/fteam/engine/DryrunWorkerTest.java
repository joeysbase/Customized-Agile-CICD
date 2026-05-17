package fteam.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fteam.testutil.PipelineFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests dryrun worker output for valid and invalid pipelines. */
class DryrunWorkerTest {

  private List<String> messages;
  private AtomicBoolean status;

  @BeforeEach
  void setUp() {
    messages = new ArrayList<>();
    status = new AtomicBoolean(false);
  }

  private DryrunWorker workerFor(String yaml) {
    DryrunWorker worker = DryrunWorker.fromFileString(yaml);
    worker.setMessages(messages);
    worker.setStatus(status);
    return worker;
  }

  @Test
  void validYamlEmitsPlanDetails() {
    workerFor(PipelineFixtures.VALID_SINGLE_JOB).run();
    assertTrue(status.get());
    assertTrue(messages.stream().anyMatch(m -> m.contains("OK")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("simple-pipeline")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("build")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("compile")));
  }

  @Test
  void validYamlWithNeedsMentionsDependencyOrder() {
    workerFor(PipelineFixtures.VALID_WITH_NEEDS).run();
    assertTrue(messages.stream().anyMatch(m -> m.contains("unit-test")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("integration-test")));
  }

  @Test
  void invalidYamlProducesErrorsAndNoOkBanner() {
    workerFor(PipelineFixtures.INVALID_MISSING_STAGES).run();
    assertFalse(messages.isEmpty());
    assertFalse(messages.stream().anyMatch(m -> m.startsWith("OK")));
  }
}
