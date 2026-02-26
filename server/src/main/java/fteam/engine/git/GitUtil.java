package fteam.engine.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class GitUtil {

  public static String currentBranch(File repoDir) throws Exception {
    return run(repoDir, "git", "-C", repoDir.getAbsolutePath(), "branch", "--show-current").trim();
  }

  public static String currentCommit(File repoDir) throws Exception {
    return run(repoDir, "git", "-C", repoDir.getAbsolutePath(), "rev-parse", "HEAD").trim();
  }

  // latest commit on branch == HEAD of current checkout (since we don't allow switching)
  private static String run(File repoDir, String... cmd) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(true);
    Process p = pb.start();

    StringBuilder sb = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      String line;
      while ((line = br.readLine()) != null) sb.append(line).append("\n");
    }
    int code = p.waitFor();
    if (code != 0) throw new RuntimeException("Command failed: " + String.join(" ", cmd));
    return sb.toString();
  }
}
