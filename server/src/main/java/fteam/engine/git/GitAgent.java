package fteam.engine.git;

import java.io.File;

public interface GitAgent {
  /**
   * Prepare an execution workspace directory that contains the repo content
   * at the requested branch/commit.
   */
  File prepareWorkspace(String repoPath, String branch, String commit) throws Exception;
}

