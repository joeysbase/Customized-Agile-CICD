package fteam.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fteam.engine.ReportService;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP handler for query-parameter-based report requests.
 */
public class ReportHandler implements HttpHandler {

  /**
   * Validates report query parameters and writes the matching report response.
   *
   * @param exchange request/response exchange
   * @throws IOException when request handling fails
   */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      writeText(exchange, 405, "ERROR: Method Not Allowed");
      return;
    }

    Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
    String pipeline = q.get("pipeline");
    String runStr = q.get("run");
    String stage = q.get("stage");
    String job = q.get("job");

    if (pipeline == null || pipeline.isBlank()) {
      writeText(exchange, 400, "ERROR: Missing required query param: pipeline");
      return;
    }

    if (stage != null && runStr == null) {
      writeText(exchange, 400, "ERROR: stage requires run");
      return;
    }

    if (job != null && stage == null) {
      writeText(exchange, 400, "ERROR: job requires stage");
      return;
    }

    try {
      String body = ReportService.renderReport(pipeline, runStr, stage, job);
      writeText(exchange, 200, body);
    } catch (NumberFormatException e) {
      writeText(exchange, 400, "ERROR: run must be an integer");
    } catch (Exception e) {
      writeText(exchange, 500, "ERROR: Server error: " + e.getMessage());
    }
  }

  private static void writeText(HttpExchange ex, int code, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
    ex.sendResponseHeaders(code, bytes.length);
    ex.getResponseBody().write(bytes);
    ex.close();
  }

  private static Map<String, String> parseQuery(String raw) {
    Map<String, String> map = new HashMap<>();
    if (raw == null || raw.isBlank()) {
      return map;
    }

    for (String part : raw.split("&")) {
      int idx = part.indexOf('=');
      if (idx < 0) {
        continue;
      }
      String k = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8);
      String v = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
      map.put(k, v);
    }
    return map;
  }

  /**
   * Creates a report handler.
   */
  public ReportHandler() {
  }
}
