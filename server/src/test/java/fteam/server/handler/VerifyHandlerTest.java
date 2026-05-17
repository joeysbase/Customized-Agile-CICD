package fteam.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** Tests verify handler request validation and YAML response content. */
class VerifyHandlerTest extends AbstractHandlerTest {

  private static final String PATH = "/api/pipelines/verify";

  @TempDir
  Path tempDir;

  @Override
  protected void registerHandlers(HttpServer server) {
    server.createContext(PATH, new VerifyHandler());
  }

  @Test
  void postValidYamlReturnsValidTrue() throws Exception {
    HttpResponse<String> res = post(PATH, PipelineFixtures.VALID_SINGLE_JOB);
    assertEquals(200, res.statusCode());
    assertTrue(res.body().contains("valid: true"));
    assertTrue(res.body().contains("messages:"));
    assertFalse(res.body().contains("    - "));
  }

  @Test
  void postInvalidYamlReturnsValidFalseWithMessages() throws Exception {
    HttpResponse<String> res = post(PATH, PipelineFixtures.INVALID_MISSING_PIPELINE_SECTION);
    assertEquals(200, res.statusCode());
    assertTrue(res.body().contains("valid: false"));
    assertTrue(res.body().contains("    - "));
  }

  @Test
  void undefinedStageMentionsBadStageName() throws Exception {
    HttpResponse<String> res = post(PATH, PipelineFixtures.INVALID_UNDEFINED_STAGE);
    assertTrue(res.body().contains("nonexistent"));
  }

  @Test
  void getValidYamlFileReturnsValidTrue() throws Exception {
    Path yamlFile = tempDir.resolve("verify pipeline.yaml");
    Files.writeString(yamlFile, PipelineFixtures.VALID_SINGLE_JOB);

    String encoded = URLEncoder.encode(yamlFile.toString(), StandardCharsets.UTF_8);
    HttpResponse<String> res = get(PATH + "?unused&file=" + encoded);

    assertEquals(200, res.statusCode());
    assertTrue(res.body().contains("valid: true"));
    assertTrue(res.body().contains("messages:"));
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
