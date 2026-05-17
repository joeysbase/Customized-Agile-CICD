package fteam.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import fteam.testutil.PipelineFixtures;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests dryrun handler request validation and YAML plan rendering. */
class DryrunHandlerTest extends AbstractHandlerTest {

  private static final String PATH = "/api/pipelines/dryrun";

  @TempDir
  Path tempDir;

  @Override
  protected void registerHandlers(HttpServer server) {
    server.createContext(PATH, new DryrunHandler());
  }

  @Test
  void postValidYamlReturnsPlan() throws Exception {
    HttpResponse<String> res = post(PATH, PipelineFixtures.VALID_SINGLE_JOB);
    assertEquals(200, res.statusCode());
    assertTrue(res.body().contains("valid: true"));
    assertTrue(res.body().contains("plan:"));
    assertTrue(res.body().contains("stage: build"));
    assertTrue(res.body().contains("job: compile"));
  }

  @Test
  void postValidYamlWithNeedsRespectsOrder() throws Exception {
    HttpResponse<String> res = post(PATH, PipelineFixtures.VALID_WITH_NEEDS);
    String body = res.body();
    assertTrue(body.indexOf("job: unit-test") < body.indexOf("job: integration-test"));
  }

  @Test
  void postInvalidYamlReturnsMessages() throws Exception {
    HttpResponse<String> res = post(PATH, PipelineFixtures.INVALID_UNDEFINED_STAGE);
    assertEquals(200, res.statusCode());
    assertTrue(res.body().contains("valid: false"));
    assertTrue(res.body().contains("messages:"));
    assertTrue(res.body().contains("nonexistent"));
  }

  @Test
  void getValidYamlFileReturnsPlan() throws Exception {
    Path yamlFile = tempDir.resolve("dryrun pipeline.yaml");
    Files.writeString(yamlFile, PipelineFixtures.VALID_WITH_NEEDS);

    String encoded = URLEncoder.encode(yamlFile.toString(), StandardCharsets.UTF_8);
    HttpResponse<String> res = get(PATH + "?unused&file=" + encoded);

    assertEquals(200, res.statusCode());
    assertTrue(res.body().contains("valid: true"));
    assertTrue(res.body().contains("job: unit-test"));
    assertTrue(res.body().contains("job: integration-test"));
  }

  @Test
  void getBlankFileParamIsRejected() throws Exception {
    assertEquals(400, get(PATH + "?file=").statusCode());
  }

  @Test
  void emptyBodyAndWrongMethodAreRejected() throws Exception {
    assertEquals(400, post(PATH, "").statusCode());
    assertEquals(405, sendMethod("DELETE", PATH).statusCode());
    assertEquals(400, get(PATH).statusCode());
  }
}
