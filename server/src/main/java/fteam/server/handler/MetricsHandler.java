package fteam.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fteam.engine.MetricsRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * HTTP handler that exposes Prometheus metrics.
 */
public class MetricsHandler implements HttpHandler {

  /**
   * Writes the current metrics registry in Prometheus text format.
   *
   * @param exchange request/response exchange
   * @throws IOException when request handling fails
   */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      byte[] err = "Method Not Allowed".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(405, err.length);
      exchange.getResponseBody().write(err);
      exchange.close();
      return;
    }

    StringWriter writer = new StringWriter();
    TextFormat.write004(writer, MetricsRegistry.getInstance().getRegistry().metricFamilySamples());
    byte[] body = writer.toString().getBytes(StandardCharsets.UTF_8);

    exchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }

  /**
   * Creates a metrics handler.
   */
  public MetricsHandler() {
  }
}
