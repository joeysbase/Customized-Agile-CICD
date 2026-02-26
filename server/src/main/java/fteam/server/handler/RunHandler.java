package fteam.server.handler;

import fteam.engine.git.GitUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.File;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import fteam.engine.PipelineConfig;
import fteam.engine.RunWorker;

public class RunHandler implements HttpHandler {

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String query = exchange.getRequestURI().getQuery();
    System.out.println("RUN query=" + exchange.getRequestURI().getQuery());
    String file = getQueryParam(query, "file");
    String repo = getQueryParam(query, "repo");
    String branch = getQueryParam(query, "branch");
    String commit = getQueryParam(query, "commit");

    if (branch == null || branch.isEmpty()) branch = "main";
    if (commit == null || commit.isEmpty()) commit = "latest";

    if (file == null || file.isEmpty()) {
      writeJson(exchange, 400, "{ \"error\": \"missing query param: file\" }");
      return;
    }
    if (repo == null || repo.isEmpty()) {
      writeJson(exchange, 400, "{ \"error\": \"missing query param: repo\" }");
      return;
    }

    // ✅ Sprint3 spec: forbid switching branch/commit
    try {
      String currentBranch = GitUtil.currentBranch(new File(repo));
      String currentCommit = GitUtil.currentCommit(new File(repo));

      if (!branch.equals(currentBranch)) {
        writeJson(exchange, 400,
            "{ \"error\": \"requested branch " + branch + " but current branch is " + currentBranch
                + ". Please checkout " + branch + " first.\" }");
        return;
      }

      if (!"latest".equals(commit) && !commit.equals(currentCommit)) {
        writeJson(exchange, 400,
            "{ \"error\": \"requested commit " + commit + " but current HEAD is " + currentCommit
                + ". Please checkout the requested commit first.\" }");
        return;
      }
    } catch (Exception e) {
      writeJson(exchange, 500, "{ \"error\": \"git check failed: " + e.getMessage() + "\" }");
      return;
    }

    PipelineConfig cfg = PipelineConfig.fromFile(file);
    if (!cfg.isvalidConfig()) {
      String resp = "{ \"valid\": false, \"messages\": " + cfg.getVerificationMsg().toString() + " }";
      writeJson(exchange, 200, resp);
      return;
    }

    RunWorker worker = RunWorker.fromFileString(file, new File(repo));
    worker.run();

    String resp = "{ \"valid\": true, \"messages\": " + worker.getMessages().toString() + " }";
    writeJson(exchange, 200, resp);
  }
  // --- helpers ---

  private static void writeJson(HttpExchange exchange, int code, String json) throws IOException {
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(code, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private static String getQueryParam(String query, String key) {
    if (query == null) return null;
    for (String part : query.split("&")) {
      String[] kv = part.split("=", 2);
      if (kv.length == 2 && kv[0].equals(key)) return kv[1];
    }
    return null;
  }
}
