package fteam.server.handler;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import fteam.engine.PipelineConfig;

public class VerifyHandler implements HttpHandler {

  @Override
  public void handle(HttpExchange exchange) throws IOException {

    String query = exchange.getRequestURI().getQuery(); // file=xxx
    String file = query.split("=")[1];


    PipelineConfig cfg = PipelineConfig.fromFile(file);

    String resp =
        "{ \"valid\": " + cfg.isvalidConfig() +
            ", \"messages\": " + cfg.getVerificationMsg().toString() + "}";

    exchange.sendResponseHeaders(200, resp.getBytes().length);
    exchange.getResponseBody().write(resp.getBytes());
    exchange.close();
  }
}

