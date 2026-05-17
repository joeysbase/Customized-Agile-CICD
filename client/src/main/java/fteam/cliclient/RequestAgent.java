package fteam.cliclient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small HTTP client wrapper used by the CLI to communicate with the server. */
public class RequestAgent {

  /** Shared message buffer populated with the latest server response. */
  public static final List<String> MESSAGES = new ArrayList<>();

  private static final String BASE_URL =
      System.getenv().getOrDefault("CICD_SERVER_URL", "http://localhost:8080");
  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  /** Creates a request agent wrapper for static CLI HTTP helpers. */
  public RequestAgent() {}

  /**
   * Sends a GET-based verify request using a file path.
   *
   * @param filePath path to the pipeline configuration file
   */
  public static void requestVerifyWorker(String filePath) {
    String url = BASE_URL + "/api/pipelines/verify?file=" + enc(filePath);
    request(url);
  }

  /**
   * Sends a POST-based verify request using inline YAML content.
   *
   * @param yamlContent pipeline configuration content
   */
  public static void postVerifyWorker(String yamlContent) {
    String url = BASE_URL + "/api/pipelines/verify";
    post(url, yamlContent);
  }

  /**
   * Sends a GET-based dry-run request using a file path.
   *
   * @param filePath path to the pipeline configuration file
   */
  public static void requestDryrunWorker(String filePath) {
    String url = BASE_URL + "/api/pipelines/dryrun?file=" + enc(filePath);
    request(url);
  }

  /**
   * Sends a POST-based dry-run request using inline YAML content.
   *
   * @param yamlContent pipeline configuration content
   */
  public static void postDryrunWorker(String yamlContent) {
    String url = BASE_URL + "/api/pipelines/dryrun";
    post(url, yamlContent);
  }

  /**
   * Sends a GET-based run request using a pipeline file path and Git selection.
   *
   * @param filePath path to the pipeline configuration file
   * @param repo repository path
   * @param branch branch name
   * @param commit commit hash or alias
   */
  public static void requestRunWorker(String filePath, String repo, String branch, String commit) {
    String url =
        BASE_URL
            + "/api/pipelines/run?file="
            + enc(filePath)
            + "&repo="
            + enc(repo)
            + "&branch="
            + enc(branch)
            + "&commit="
            + enc(commit);
    request(url);
  }

  /**
   * Sends a POST-based run request using inline YAML content and Git selection.
   *
   * @param yamlContent pipeline configuration content
   * @param repo repository path
   * @param branch branch name
   * @param commit commit hash or alias
   */
  public static void postRunWorker(String yamlContent, String repo, String branch, String commit) {
    String url =
        BASE_URL
            + "/api/pipelines/run?repo="
            + enc(repo)
            + "&branch="
            + enc(branch)
            + "&commit="
            + enc(commit);
    post(url, yamlContent);
  }

  /**
   * Sends a legacy GET-based report request using query parameters.
   *
   * @param pipelineName pipeline name
   * @param runNo run number filter
   * @param stageName stage name filter
   * @param jobName job name filter
   */
  public static void requestReportWorker(
      String pipelineName, String runNo, String stageName, String jobName) {

    StringBuilder url =
        new StringBuilder(BASE_URL + "/api/pipelines/report?pipeline=" + enc(pipelineName));

    if (runNo != null && !runNo.isBlank()) {
      url.append("&run=").append(enc(runNo));
    }
    if (stageName != null && !stageName.isBlank()) {
      url.append("&stage=").append(enc(stageName));
    }
    if (jobName != null && !jobName.isBlank()) {
      url.append("&job=").append(enc(jobName));
    }

    request(url.toString());
  }

  /**
   * Sends a path-based report request using the current REST route layout.
   *
   * @param pipelineName pipeline name
   * @param runNo optional run number
   * @param stageName optional stage name
   * @param jobName optional job name
   */
  public static void getReportByPath(
      String pipelineName, String runNo, String stageName, String jobName) {

    StringBuilder url =
        new StringBuilder(BASE_URL + "/api/pipelines/" + enc(pipelineName) + "/runs");

    if (runNo != null && !runNo.isBlank()) {
      url.append("/").append(enc(runNo));
    }
    if (stageName != null && !stageName.isBlank()) {
      url.append("/").append(enc(stageName));
    }
    if (jobName != null && !jobName.isBlank()) {
      url.append("/").append(enc(jobName));
    }

    request(url.toString());
  }

  private static void request(String url) {
    MESSAGES.clear();

    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

    try {
      HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

      MESSAGES.add(response.body());

    } catch (IOException | InterruptedException e) {
      MESSAGES.add("ERROR: " + e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  private static void post(String url, String body) {
    MESSAGES.clear();

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "text/plain; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();

    try {
      HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

      MESSAGES.add(response.body());

    } catch (IOException | InterruptedException e) {
      MESSAGES.add("ERROR: " + e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  private static String enc(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }
}
