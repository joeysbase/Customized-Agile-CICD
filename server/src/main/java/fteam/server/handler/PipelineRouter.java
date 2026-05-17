package fteam.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fteam.engine.ReportService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Path-based router for report endpoints under {@code /api/pipelines/...}.
 */
public class PipelineRouter implements HttpHandler {

  private static final String PREFIX = "/api/pipelines/";

  /**
   * Creates a router for path-based report requests.
   */
  public PipelineRouter() {
  }

  /**
   * Routes a path-based report request to the report service.
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

    String path = exchange.getRequestURI().getPath();
    if (!path.startsWith(PREFIX)) {
      writeText(exchange, 404, "ERROR: Not Found");
      return;
    }

    String[] parts = path.substring(PREFIX.length()).split("/", -1);

    if (parts.length < 2 || !"runs".equals(parts[1])) {
      writeText(exchange, 404, "ERROR: Not Found");
      return;
    }

    String pipelineName = parts[0];
    String runNo = parts.length > 2 ? parts[2] : null;
    String stage = parts.length > 3 ? parts[3] : null;
    String job = parts.length > 4 ? parts[4] : null;

    if (pipelineName.isBlank()) {
      writeText(exchange, 400, "ERROR: pipeline name is required");
      return;
    }

    try {
      String body = ReportService.renderReport(pipelineName, runNo, stage, job);
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
}
