package fteam.engine.git;

import java.io.File;

/**
 * Git agent that executes directly against the existing local repository checkout.
 */
public class LocalGitAgent implements GitAgent {

  /**
   * Creates a Git agent that reuses the local repository checkout.
   */
  public LocalGitAgent() {
  }

  /**
   * Returns the local repository directory without creating a separate workspace.
   *
   * @param repoPath repository path
   * @param branch ignored in the current local execution mode
   * @param commit ignored in the current local execution mode
   * @return local repository directory
   */
  @Override
  public File prepareWorkspace(String repoPath, String branch, String commit) {
    // Sprint3: ignore branch/commit, execute directly in local repo
    return new File(repoPath);
  }
}
