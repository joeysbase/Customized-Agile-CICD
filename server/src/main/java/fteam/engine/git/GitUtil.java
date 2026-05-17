package fteam.engine.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Helper methods for querying Git state from a local repository.
 */
public class GitUtil {

  /**
   * Creates a utility holder for static Git helper methods.
   */
  public GitUtil() {
  }

  /**
   * Returns the current checked-out branch name.
   *
   * @param repoDir repository directory
   * @return current branch name
   * @throws Exception when Git invocation fails
   */
  public static String currentBranch(File repoDir) throws Exception {
    return run(repoDir, "git", "-C", repoDir.getAbsolutePath(), "branch", "--show-current")
        .trim();
  }

  /**
   * Returns the current checked-out commit hash.
   *
   * @param repoDir repository directory
   * @return current commit hash
   * @throws Exception when Git invocation fails
   */
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
      String line = br.readLine();
      while (line != null) {
        sb.append(line).append("\n");
        line = br.readLine();
      }
    }
    int code = p.waitFor();
    if (code != 0) {
      throw new RuntimeException("Command failed: " + String.join(" ", cmd));
    }
    return sb.toString();
  }
}
