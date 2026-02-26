package fteam.engine.git;

import java.io.File;
import java.nio.file.Files;

public class WorkspaceManager {

  public File createWorkspace() throws Exception {
    return Files.createTempDirectory("fteam-workspace-").toFile();
  }

  public void cleanup(File workspace) {

  }
}

