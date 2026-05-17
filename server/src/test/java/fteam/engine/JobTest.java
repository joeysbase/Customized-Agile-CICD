package fteam.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests YAML-based job construction and defaults. */
class JobTest {

  private static final String ALPINE = "alpine";
  private static final String BUILD = "build";
  private static final String ECHO_HELLO = "echo hello";
  private static final String JOB_NAME = "my-job";
  private static final String TEST = "test";

  private Map<String, Object> jobMap(
      String stage, String image, Object script, Object needs, Object failures) {
    Map<String, Object> map = new HashMap<>();
    map.put("stage", stage);
    if (image != null) {
      map.put("image", image);
    }
    if (script != null) {
      map.put("script", script);
    }
    if (needs != null) {
      map.put("needs", needs);
    }
    if (failures != null) {
      map.put("failures", failures);
    }
    return map;
  }

  @Test
  void fromYamlMapsNameAndStage() {
    Job job = Job.fromYaml(JOB_NAME, jobMap(BUILD, ALPINE, "echo hi", null, null));
    assertEquals(JOB_NAME, job.getName());
    assertEquals(BUILD, job.getStage());
  }

  @Test
  void fromYamlHandlesSingleStringScript() {
    Job job = Job.fromYaml(JOB_NAME, jobMap(BUILD, ALPINE, ECHO_HELLO, null, null));
    assertEquals(List.of(ECHO_HELLO), job.getScripts());
  }

  @Test
  void fromYamlHandlesListScriptAndNeeds() {
    Job job =
        Job.fromYaml(
            "test-job",
            jobMap(TEST, ALPINE, List.of("echo a", "echo b"), List.of("compile"), null));
    assertEquals(List.of("echo a", "echo b"), job.getScripts());
    assertEquals(List.of("compile"), job.getNeeds());
  }

  @Test
  void failuresDefaultsToFalseAndCanBeEnabled() {
    Job strict = Job.fromYaml("strict", jobMap(TEST, ALPINE, "echo ok", null, null));
    Job allowed = Job.fromYaml("allowed", jobMap(TEST, ALPINE, "exit 1", null, Boolean.TRUE));
    assertFalse(strict.isFailureAllowed());
    assertTrue(allowed.isFailureAllowed());
  }

  @Test
  void invalidFailuresTypeThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Job.fromYaml("bad", jobMap(TEST, ALPINE, "echo ok", null, "yes")));
  }

  @Test
  void freshJobStartsPending() {
    Job job = Job.fromYaml(JOB_NAME, jobMap(BUILD, null, "echo hi", null, null));
    assertEquals(Job.JobStatus.PENDING, job.getStatus());
    assertFalse(job.isCompleted());
    assertNull(job.getImage());
  }

  @Test
  void localExecutionWithoutImageCanSucceed(@TempDir Path tempDir) throws InterruptedException {
    Job job = Job.fromYaml("local-job", jobMap(BUILD, null, ECHO_HELLO, null, null));
    CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<>();
    job.setRepoDir(tempDir.toFile());
    job.setCallback(messages::add);
    job.setPipelineName("test-pipeline");

    job.run();
    job.waitForCompletion();

    assertTrue(job.isCompleted());
    assertTrue(job.isSuccess());
    assertTrue(messages.stream().anyMatch(m -> m.contains("No image specified")));
    assertTrue(messages.stream().anyMatch(m -> m.contains("hello")));
    assertEquals("local-job", job.getName());
    assertEquals(BUILD, job.getStage());
    assertEquals(List.of(ECHO_HELLO), job.getScripts());
    assertEquals(ECHO_HELLO, job.getScript()[0]);
    assertTrue(job.getStartTime() != null);
    assertTrue(job.getEndTime() != null);
  }

  @Test
  void localExecutionFailureSetsFailedStatus(@TempDir Path tempDir) throws InterruptedException {
    Job job = Job.fromYaml("bad-job", jobMap(BUILD, null, "exit 1", null, null));
    job.setRepoDir(tempDir.toFile());
    job.setCallback(msg -> {});
    job.setPipelineName("test-pipeline");

    job.run();
    job.waitForCompletion();

    assertTrue(job.isFailed());
    assertFalse(job.isSuccess());
    assertTrue(job.getErrorMessage().contains("exit code"));
  }
}
