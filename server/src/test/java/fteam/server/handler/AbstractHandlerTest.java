package fteam.server.handler;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/** Shared HTTP test harness for exercising handlers with a real HttpServer. */
abstract class AbstractHandlerTest {

  protected HttpServer server;
  protected HttpClient client;
  protected int port;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    registerHandlers(server);
    server.setExecutor(Executors.newSingleThreadExecutor());
    server.start();
    port = server.getAddress().getPort();
    client = HttpClient.newHttpClient();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  protected abstract void registerHandlers(HttpServer server);

  protected HttpResponse<String> post(String path, String body) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "text/plain; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(req, HttpResponse.BodyHandlers.ofString());
  }

  protected HttpResponse<String> get(String path) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build();
    return client.send(req, HttpResponse.BodyHandlers.ofString());
  }

  protected HttpResponse<String> sendMethod(String method, String path) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + path))
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build();
    return client.send(req, HttpResponse.BodyHandlers.ofString());
  }
}
