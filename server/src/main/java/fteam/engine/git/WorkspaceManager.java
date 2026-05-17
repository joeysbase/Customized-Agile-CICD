package fteam.engine.git;

import java.io.File;
import java.nio.file.Files;

/**
 * Creates and cleans up temporary workspaces used during Git-backed execution.
 */
public class WorkspaceManager {

  /**
   * Creates a workspace manager for temporary repositories.
   */
  public WorkspaceManager() {
  }

  /**
   * Creates a temporary workspace directory.
   *
   * @return temporary workspace directory
   * @throws Exception when the directory cannot be created
   */
  public File createWorkspace() throws Exception {
    return Files.createTempDirectory("fteam-workspace-").toFile();
  }

  /**
   * Cleans up a previously created workspace.
   *
   * @param workspace workspace directory to clean up
   */
  public void cleanup(File workspace) {

  }
}
