package fteam.server;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Application entry point for the CI/CD server process.
 */
public class ServerMain {

  private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
  private static OtlpGrpcLogRecordExporter logExporter;
  private static SdkLoggerProvider loggerProvider;
  private static OtlpGrpcSpanExporter spanExporter;
  private static SdkTracerProvider tracerProvider;
  private static OpenTelemetrySdk sdk;

  /**
   * Creates the server application entry-point type.
   */
  public ServerMain() {
  }

  /**
   * Initializes logging and telemetry, then starts the HTTP server.
   *
   * @param args unused command-line arguments
   */
  public static void main(String[] args) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    initOpenTelemetry();
    try {
      new ServerAgent().start();
    } catch (Exception e) {
      logger.error("Failed to start server", e);
    }
  }

  private static void initOpenTelemetry() {
    String endpoint = System.getenv().getOrDefault(
        "OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317");

    Resource resource = Resource.getDefault().toBuilder()
        .put(AttributeKey.stringKey("service.name"), "cicd-server")
        .build();

    logExporter = OtlpGrpcLogRecordExporter.builder()
        .setEndpoint(endpoint)
        .build();

    loggerProvider = SdkLoggerProvider.builder()
        .setResource(resource)
        .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
        .build();

    spanExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint(endpoint)
        .build();

    tracerProvider = SdkTracerProvider.builder()
        .setResource(resource)
        .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
        .build();

    sdk = OpenTelemetrySdk.builder()
        .setLoggerProvider(loggerProvider)
        .setTracerProvider(tracerProvider)
        .buildAndRegisterGlobal();
    OpenTelemetryAppender.install(sdk);
    Runtime.getRuntime().addShutdownHook(new Thread(ServerMain::shutdownOpenTelemetry));
  }

  private static void shutdownOpenTelemetry() {
    if (sdk != null) {
      sdk.close();
    }
    if (tracerProvider != null) {
      tracerProvider.close();
    }
    if (spanExporter != null) {
      spanExporter.close();
    }
    if (loggerProvider != null) {
      loggerProvider.close();
    }
    if (logExporter != null) {
      logExporter.close();
    }
  }
}
