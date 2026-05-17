package fteam.engine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fteam.testutil.PipelineFixtures;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests verify, dryrun, and error-path run orchestration in WorkerManager. */
class WorkerManagerTest {

  private static final String GIT = "git";

  private void awaitWorker(WorkerManager wm, int id) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5_000;
    while (!wm.getWorkerStatus(id) && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
  }

  @Test
  void verifyWorkerCompletesAndCollectsMessages() throws InterruptedException {
    WorkerManager wm = new WorkerManager();
    int validId = wm.getVerifyWorker(PipelineFixtures.VALID_SINGLE_JOB);
    int invalidId = wm.getVerifyWorker(PipelineFixtures.INVALID_CYCLE);

    awaitWorker(wm, validId);
    awaitWorker(wm, invalidId);

    assertTrue(wm.getWorkerStatus(validId));
    assertTrue(wm.getWorkerMessages(validId).isEmpty());
    assertTrue(wm.getWorkerMessages(invalidId).stream().anyMatch(m -> m.contains("cycle")));
  }

  @Test
  void dryrunWorkerCompletesAndReturnsPlanMessages() throws InterruptedException {
    WorkerManager wm = new WorkerManager();
    int id = wm.getDryrunWorker(PipelineFixtures.VALID_SINGLE_JOB);
    awaitWorker(wm, id);
    assertTrue(wm.getWorkerStatus(id));
    assertTrue(wm.getWorkerMessages(id).stream().anyMatch(m -> m.contains("simple-pipeline")));
  }

  @Test
  void removeWorkerDoesNotThrow() throws InterruptedException {
    WorkerManager wm = new WorkerManager();
    int id = wm.getVerifyWorker(PipelineFixtures.VALID_SINGLE_JOB);
    awaitWorker(wm, id);
    assertDoesNotThrow(() -> wm.removeWorker(id));
  }

  @Test
  void runWorkerWithNonGitRepoReturnsError(@TempDir Path tempDir) throws InterruptedException {
    WorkerManager wm = new WorkerManager();
    int id =
        wm.getRunWorker(PipelineFixtures.VALID_SINGLE_JOB, tempDir.toString(), "latest", "main");
    awaitWorker(wm, id);
    assertTrue(wm.getWorkerStatus(id));
    assertFalse(wm.getWorkerMessages(id).isEmpty());
    assertTrue(wm.getWorkerMessages(id).stream().anyMatch(m -> m.startsWith("ERROR:")));
  }

  @Test
  void runWorkerWithBranchMismatchReturnsHelpfulError(@TempDir Path tempDir)
      throws Exception {
    initGitRepo(tempDir);

    WorkerManager wm = new WorkerManager();
    int id =
        wm.getRunWorker(PipelineFixtures.VALID_SINGLE_JOB, tempDir.toString(), "latest", "wrong");
    awaitWorker(wm, id);

    assertTrue(wm.getWorkerStatus(id));
    assertTrue(wm.getWorkerMessages(id).stream().anyMatch(m -> m.contains("requested branch")));
  }

  @Test
  void runWorkerWithCommitMismatchReturnsHelpfulError(@TempDir Path tempDir)
      throws Exception {
    initGitRepo(tempDir);

    WorkerManager wm = new WorkerManager();
    int id =
        wm.getRunWorker(
            PipelineFixtures.VALID_SINGLE_JOB, tempDir.toString(), "deadbeef", "main");
    awaitWorker(wm, id);

    assertTrue(wm.getWorkerStatus(id));
    assertTrue(wm.getWorkerMessages(id).stream().anyMatch(m -> m.contains("requested commit")));
  }

  private void initGitRepo(Path repo) throws IOException, InterruptedException {
    run(repo, GIT, "init", "-b", "main");
    run(repo, GIT, "config", "user.email", "test@example.com");
    run(repo, GIT, "config", "user.name", "Test User");
    java.nio.file.Files.writeString(repo.resolve("README.md"), "hello");
    run(repo, GIT, "add", "README.md");
    run(repo, GIT, "commit", "-m", "init");
  }

  private void run(Path cwd, String... command) throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).directory(cwd.toFile()).start();
    int exit = process.waitFor();
    if (exit != 0) {
      throw new IOException("command failed: " + String.join(" ", command));
    }
  }
}
