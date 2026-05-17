package fteam.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fteam.engine.Job;
import fteam.engine.PipelineConfig;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP handler for dry-run requests.
 */
public class DryrunHandler implements HttpHandler {

  private static final String POST_METHOD = "POST";
  private static final String GET_METHOD = "GET";

  /**
   * Dispatches dry-run requests based on the HTTP method.
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

  /**
   * Creates a dry-run handler.
   */
  public DryrunHandler() {
  }

  private void handlePost(HttpExchange exchange) throws IOException {
    String yamlContent =
        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

    if (yamlContent.isBlank()) {
      writeText(exchange, 400, "ERROR: request body is empty");
      return;
    }

    writeText(exchange, 200, buildDryrunResponse(PipelineConfig.fromYamlString(yamlContent)));
  }

  private void handleGet(HttpExchange exchange) throws IOException {
    Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
    String content = params.get("file");

    if (content == null || content.isBlank()) {
      writeText(exchange, 400, "ERROR: Missing required query parameter: file");
      return;
    }

    writeText(exchange, 200, buildDryrunResponse(PipelineConfig.fromFile(content)));
  }

  private String buildDryrunResponse(PipelineConfig cfg) {
    StringBuilder resp = new StringBuilder();

    if (!cfg.isvalidConfig()) {
      resp.append("dryrun:\n");
      resp.append("  valid: false\n");
      resp.append("  messages:\n");

      for (String msg : cfg.getVerificationMsg()) {
        resp.append("    - ").append(msg == null ? "" : msg).append("\n");
      }
    } else {
      resp.append("dryrun:\n");
      resp.append("  valid: true\n");
      resp.append("  plan:\n");

      List<Job> plan = cfg.getExcutionSequence();
      for (Job j : plan) {
        resp.append("    - stage: ").append(nzs(j.getStage())).append("\n");
        resp.append("      job: ").append(nzs(j.getName())).append("\n");
      }
    }

    return resp.toString();
  }

  private void writeText(HttpExchange exchange, int statusCode, String body) throws IOException {
    byte[] out = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
    exchange.sendResponseHeaders(statusCode, out.length);
    exchange.getResponseBody().write(out);
    exchange.close();
  }

  private Map<String, String> parseQuery(String query) {
    Map<String, String> params = new HashMap<>();
    if (query == null || query.isBlank()) {
      return params;
    }

    for (String pair : query.split("&")) {
      String[] kv = pair.split("=", 2);
      String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
      String value = kv.length > 1
          ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
          : "";
      params.put(key, value);
    }
    return params;
  }

  private String nzs(String s) {
    return s == null ? "" : s;
  }
}
