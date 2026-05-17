package fteam.engine.git;

import java.io.File;

/**
 * Strategy interface for preparing a repository workspace for pipeline execution.
 */
public interface GitAgent {
  /**
   * Prepare an execution workspace directory that contains the repo content
   * at the requested branch/commit.
   *
   * @param repoPath source repository path
   * @param branch branch to prepare, if applicable
   * @param commit commit to prepare, if applicable
   * @return prepared workspace directory
   * @throws Exception when workspace preparation fails
   */
  File prepareWorkspace(String repoPath, String branch, String commit) throws Exception;
}
