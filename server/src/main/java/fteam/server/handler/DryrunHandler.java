package fteam.server.handler;

import java.io.IOException;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import fteam.engine.Job;
import fteam.engine.PipelineConfig;

public class DryrunHandler implements HttpHandler {

  @Override
  public void handle(HttpExchange exchange) throws IOException {

    String query = exchange.getRequestURI().getQuery(); // file=xxx
    String content = query.split("=")[1];

    PipelineConfig cfg = PipelineConfig.fromFile(content);

    StringBuilder resp = new StringBuilder();

    if (!cfg.isvalidConfig()) {
      resp.append("{\"valid\":false,\"messages\":")
          .append(cfg.getVerificationMsg().toString())
          .append("}");
    } else {
      resp.append("{\"valid\":true,\"plan\":[");

      List<Job> plan = cfg.getExcutionSequence();

      for (int i = 0; i < plan.size(); i++) {
        Job j = plan.get(i);
        resp.append("{\"stage\":\"")
            .append(j.getStage())
            .append("\",\"job\":\"")
            .append(j.getName())
            .append("\"}");

        if (i < plan.size() - 1) resp.append(",");
      }

      resp.append("]}");
    }

    byte[] out = resp.toString().getBytes();
    exchange.sendResponseHeaders(200, out.length);
    exchange.getResponseBody().write(out);
    exchange.close();
  }
}
