package fteam.engine.git;

import java.io.File;

/**
 * Git agent that clones a repository into a temporary workspace and optionally checks out a
 * branch or commit.
 */
public class CheckoutGitAgent implements GitAgent {

  private final WorkspaceManager wm = new WorkspaceManager();

  /**
   * Creates a checkout-based Git agent.
   */
  public CheckoutGitAgent() {
  }

  /**
   * Prepares a fresh workspace by cloning the repository and applying the requested branch or
   * commit selection.
   *
   * @param repoPath repository path or clone source
   * @param branch optional branch to check out
   * @param commit optional commit to check out
   * @return prepared workspace directory
   * @throws Exception when cloning or checkout fails
   */
  @Override
  public File prepareWorkspace(String repoPath, String branch, String commit) throws Exception {
    File workspace = wm.createWorkspace();

    // 1) clone
    run(null, "git", "clone", repoPath, workspace.getAbsolutePath());

    // 2) checkout branch (optional)
    if (branch != null && !branch.isBlank() && !branch.equals("latest")) {
      run(workspace, "git", "checkout", branch);
    }

    // 3) checkout commit (optional, usually overrides branch)
    if (commit != null && !commit.isBlank() && !commit.equals("latest")) {
      run(workspace, "git", "checkout", commit); // detached HEAD
    }

    return workspace;
  }

  private void run(File dir, String... cmd) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(cmd);
    if (dir != null) {
      pb.directory(dir);
    }
    pb.redirectErrorStream(true);
    Process p = pb.start();
    int code = p.waitFor();
    if (code != 0) {
      throw new RuntimeException("Command failed: " + String.join(" ", cmd));
    }
  }
}
