package fteam.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fteam.engine.WorkerManager;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportHandler implements HttpHandler {

  private final WorkerManager wm;

  public ReportHandler(WorkerManager wm) {
    this.wm = wm;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      write(exchange, 405, "Method Not Allowed");
      return;
    }

    Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
    String pipeline = q.get("pipeline");
    String run = q.get("run");
    String stage = q.get("stage");
    String job = q.get("job");

    if (pipeline == null || pipeline.isBlank()) {
      write(exchange, 400, "Missing required query param: pipeline");
      return;
    }

    // spec 依赖校验
    if (stage != null && run == null) {
      write(exchange, 400, "--stage requires --run");
      return;
    }
    if (job != null && stage == null) {
      write(exchange, 400, "--job requires --stage");
      return;
    }

    int id = -1;
    try {
      // ✅ 1) 创建 worker，拿到 workerId
      id = wm.getReportWorker(pipeline, run, stage, job);

      // ✅ 2) 等 worker 完成（加 sleep 防止 busy spin）
      while (!wm.getWorkerStatus(id)) {
        try {
          Thread.sleep(5);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          write(exchange, 500, "Interrupted while waiting for report worker");
          return;
        }
      }

      // ✅ 3) 取 messages（你们框架的“结果”）
      List<String> msgs = wm.getWorkerMessages(id);
      String body = (msgs == null || msgs.isEmpty()) ? "" : String.join("\n", msgs);

      // ✅ 4) 清理 worker
      wm.removeWorker(id);

      // ✅ 5) 返回给 client
      write(exchange, 200, body);

    } catch (IllegalArgumentException e) {
      if (id != -1) wm.removeWorker(id);
      write(exchange, 400, "Bad request: " + e.getMessage());
    } catch (Exception e) {
      if (id != -1) wm.removeWorker(id);
      write(exchange, 500, "Server error: " + e.getMessage());
    }
  }

  private static void write(HttpExchange ex, int code, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
    ex.sendResponseHeaders(code, bytes.length);
    ex.getResponseBody().write(bytes);
    ex.close();
  }

  private static Map<String, String> parseQuery(String raw) {
    Map<String, String> map = new HashMap<>();
    if (raw == null || raw.isBlank()) return map;

    for (String part : raw.split("&")) {
      int idx = part.indexOf('=');
      if (idx < 0) continue;
      String k = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8);
      String v = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
      map.put(k, v);
    }
    return map;
  }
}