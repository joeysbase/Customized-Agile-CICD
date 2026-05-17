package fteam.cliclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests that the CLI request helper hits the expected REST routes. */
class RequestAgentTest {

  private static HttpServer testServer;
  private static final AtomicReference<String> LAST_PATH = new AtomicReference<>("");
  private static final AtomicReference<String> LAST_METHOD = new AtomicReference<>("");
  private static final AtomicReference<String> LAST_BODY = new AtomicReference<>("");
  private static final byte[] RESPONSE = "ok".getBytes(StandardCharsets.UTF_8);

  @BeforeAll
  static void startTestServer() throws IOException {
    testServer = HttpServer.create(new InetSocketAddress(8080), 0);
    testServer.createContext("/", exchange -> {
      LAST_PATH.set(exchange.getRequestURI().toString());
      LAST_METHOD.set(exchange.getRequestMethod());
      LAST_BODY.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      exchange.sendResponseHeaders(200, RESPONSE.length);
      exchange.getResponseBody().write(RESPONSE);
      exchange.close();
    });
    testServer.setExecutor(Executors.newSingleThreadExecutor());
    testServer.start();
  }

  @AfterAll
  static void stopTestServer() {
    testServer.stop(0);
  }

  @BeforeEach
  void clearMessages() {
    RequestAgent.MESSAGES.clear();
  }

  @Test
  void requestVerifyWorkerUsesVerifyEndpoint() {
    RequestAgent.requestVerifyWorker("/some/pipeline.yaml");
    assertTrue(LAST_PATH.get().contains("/api/pipelines/verify"));
    assertTrue("GET".equalsIgnoreCase(LAST_METHOD.get()));
    assertFalse(RequestAgent.MESSAGES.isEmpty());
  }

  @Test
  void postDryrunWorkerUsesDryrunEndpoint() {
    RequestAgent.postDryrunWorker("yaml-content");
    assertTrue(LAST_PATH.get().contains("/api/pipelines/dryrun"));
    assertTrue("POST".equalsIgnoreCase(LAST_METHOD.get()));
    assertTrue(LAST_BODY.get().contains("yaml-content"));
  }

  @Test
  void requestRunWorkerIncludesGitParameters() {
    RequestAgent.requestRunWorker("/pipe.yaml", "/repo", "main", "abc123");
    String path = LAST_PATH.get();
    assertTrue(path.contains("/api/pipelines/run"));
    assertTrue(path.contains("repo="));
    assertTrue(path.contains("branch="));
    assertTrue(path.contains("commit="));
  }

  @Test
  void getReportByPathBuildsRunsPath() {
    RequestAgent.getReportByPath("pipe", "2", "build", "compile");
    String path = LAST_PATH.get();
    assertTrue(path.contains("/api/pipelines/pipe/runs/2/build/compile"));
  }
}
