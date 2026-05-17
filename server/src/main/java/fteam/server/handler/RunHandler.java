package fteam.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fteam.engine.PipelineConfig;
import fteam.engine.WorkerManager;
import fteam.engine.git.GitUtil;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP handler for pipeline run requests. */
public class RunHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(RunHandler.class);
  private static final String POST_METHOD = "POST";
  private static final String GET_METHOD = "GET";
  private final WorkerManager wm;

  /**
   * Creates a run handler backed by the shared worker manager.
   *
   * @param wm worker manager used to start and poll run workers
   */
  public RunHandler(WorkerManager wm) {
    this.wm = wm;
  }

  /**
   * Dispatches run requests based on the HTTP method.
   *
   * @param exchange request/response exchange
   * @throws IOException when request handling fails
   */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();

    if (POST_METHOD.equalsIgnoreCase(method)) {
      handlePost(exchange);
    } else if (GET_METHOD.equalsIgnoreCase(method)) {
      handleGet(exchange);
    } else {
      writeText(exchange, 405, "ERROR: Method Not Allowed");
    }
  }

  private void handlePost(HttpExchange exchange) throws IOException {
    String query = exchange.getRequestURI().getQuery();
    String repo = getQueryParam(query, "repo");
    String branch = getQueryParam(query, "branch");
    String commit = getQueryParam(query, "commit");

    if (branch == null || branch.isEmpty()) {
      branch = "main";
    }
    if (commit == null || commit.isEmpty()) {
      commit = "latest";
    }

    if (!fteam.engine.KubernetesJobExecutor.isInCluster()) {
      if (repo == null || repo.isEmpty()) {
        writeText(exchange, 400, "ERROR: missing query param: repo");
        return;
      }
    } else {
      if (repo == null) {
        repo = "";
      }
    }

    String yamlContent =
        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    if (yamlContent.isBlank()) {
      writeText(exchange, 400, "ERROR: request body is empty");
      return;
    }

    executeRun(exchange, yamlContent, repo, branch, commit);
  }

  private void handleGet(HttpExchange exchange) throws IOException {
    String query = exchange.getRequestURI().getQuery();
    logger.info("RUN query={}", query);

    String file = getQueryParam(query, "file");
    String branch = getQueryParam(query, "branch");
    String commit = getQueryParam(query, "commit");

    if (branch == null || branch.isEmpty()) {
      branch = "main";
    }
    if (commit == null || commit.isEmpty()) {
      commit = "latest";
    }

    if (file == null || file.isEmpty()) {
      writeText(exchange, 400, "ERROR: missing query param: file");
      return;
    }
    String repo = getQueryParam(query, "repo");
    if (repo == null || repo.isEmpty()) {
      writeText(exchange, 400, "ERROR: missing query param: repo");
      return;
    }

    executeRun(exchange, file, repo, branch, commit);
  }

  private void executeRun(
      HttpExchange exchange, String content, String repo, String branch, String commit)
      throws IOException {

    if (!fteam.engine.KubernetesJobExecutor.isInCluster()) {
      try {
        String currentBranch = GitUtil.currentBranch(new File(repo));
        String currentCommit = GitUtil.currentCommit(new File(repo));

        if (!branch.equals(currentBranch)) {
          writeText(
              exchange,
              400,
              "ERROR: requested branch "
                  + branch
                  + " but current branch is "
                  + currentBranch
                  + ". Please checkout "
                  + branch
                  + " first.");
          return;
        }

        if (!"latest".equals(commit) && !commit.equals(currentCommit)) {
          writeText(
              exchange,
              400,
              "ERROR: requested commit "
                  + commit
                  + " but current HEAD is "
                  + currentCommit
                  + ". Please checkout the requested commit first.");
          return;
        }
      } catch (Exception e) {
        writeText(exchange, 500, "ERROR: git check failed: " + e.getMessage());
        return;
      }
    }

    PipelineConfig cfg = PipelineConfig.fromFile(content);
    if (!cfg.isvalidConfig()) {
      writeText(exchange, 200, renderMessages(false, cfg.getVerificationMsg()));
      return;
    }

    int id = -1;
    try {
      id = wm.getRunWorker(content, repo, commit, branch);

      while (!wm.getWorkerStatus(id)) {
        try {
          Thread.sleep(5);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          writeText(exchange, 500, "ERROR: interrupted while waiting for run worker");
          return;
        }
      }

      writeText(exchange, 200, renderMessages(true, wm.getWorkerMessages(id)));
      wm.removeWorker(id);

    } catch (Exception e) {
      if (id != -1) {
        wm.removeWorker(id);
      }
      writeText(exchange, 500, "ERROR: server error: " + e.getMessage());
    }
  }

  private static String renderMessages(boolean valid, List<String> messages) {
    StringBuilder sb = new StringBuilder();
    sb.append("run:\n");
    sb.append("  valid: ").append(valid).append("\n");
    sb.append("  messages:\n");
    for (String msg : messages) {
      sb.append("    - ").append(msg == null ? "" : msg).append("\n");
    }
    return sb.toString();
  }

  private static void writeText(HttpExchange exchange, int code, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
    exchange.sendResponseHeaders(code, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private static String getQueryParam(String query, String key) {
    if (query == null) {
      return null;
    }
    for (String part : query.split("&")) {
      String[] kv = part.split("=", 2);
      if (kv.length == 2 && kv[0].equals(key)) {
        return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
      }
    }
    return null;
  }
}
