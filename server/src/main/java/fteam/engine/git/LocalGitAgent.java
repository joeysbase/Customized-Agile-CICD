package fteam.engine.git;

import java.io.File;

public class LocalGitAgent implements GitAgent {

  @Override
  public File prepareWorkspace(String repoPath, String branch, String commit) {
    // Sprint3: ignore branch/commit, execute directly in local repo
    return new File(repoPath);
  }
}
