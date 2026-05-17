package fteam.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fteam.engine.PipelineConfig;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP handler for pipeline verification requests.
 */
public class VerifyHandler implements HttpHandler {

  private static final String POST_METHOD = "POST";
  private static final String GET_METHOD = "GET";

  /**
   * Dispatches verification requests based on the HTTP method.
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
    String yamlContent =
        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

    if (yamlContent.isBlank()) {
      writeText(exchange, 400, "ERROR: request body is empty");
      return;
    }

    PipelineConfig cfg = PipelineConfig.fromYamlString(yamlContent);
    writeText(exchange, 200, renderVerify(cfg));
  }

  private void handleGet(HttpExchange exchange) throws IOException {
    Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
    String file = params.get("file");

    if (file == null || file.isBlank()) {
      writeText(exchange, 400, "ERROR: missing query param: file");
      return;
    }

    PipelineConfig cfg = PipelineConfig.fromFile(file);
    writeText(exchange, 200, renderVerify(cfg));
  }

  private static String renderVerify(PipelineConfig cfg) {
    StringBuilder sb = new StringBuilder();
    sb.append("verify:\n");
    sb.append("  valid: ").append(cfg.isvalidConfig()).append("\n");
    sb.append("  messages:\n");

    for (String msg : cfg.getVerificationMsg()) {
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

  private static Map<String, String> parseQuery(String raw) {
    Map<String, String> map = new HashMap<>();
    if (raw == null || raw.isBlank()) {
      return map;
    }

    for (String part : raw.split("&")) {
      String[] kv = part.split("=", 2);
      String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
      String value = kv.length > 1
              ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
              : "";
      map.put(key, value);
    }
    return map;
  }

  /**
   * Creates a verification handler.
   */
  public VerifyHandler() {
  }
}
