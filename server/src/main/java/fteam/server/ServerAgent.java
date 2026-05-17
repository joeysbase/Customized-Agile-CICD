package fteam.server;

import com.sun.net.httpserver.HttpServer;
import fteam.engine.WorkerManager;
import fteam.server.handler.DryrunHandler;
import fteam.server.handler.MetricsHandler;
import fteam.server.handler.PipelineRouter;
import fteam.server.handler.ReportHandler;
import fteam.server.handler.RunHandler;
import fteam.server.handler.VerifyHandler;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Configures and starts the embedded HTTP server for the CI/CD service.
 */
public class ServerAgent {

  private static final Logger logger = LoggerFactory.getLogger(ServerAgent.class);

  /**
   * Creates a server agent for the embedded HTTP service.
   */
  public ServerAgent() {
  }

  /**
   * Creates HTTP routes, configures the executor, and starts the server.
   *
   * @throws Exception when the HTTP server cannot be created or started
   */
  public void start() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    WorkerManager wm = new WorkerManager();
    server.createContext("/api/pipelines/verify", new VerifyHandler());
    server.createContext("/api/pipelines/dryrun", new DryrunHandler());
    server.createContext("/api/pipelines/run", new RunHandler(wm));
    server.createContext("/api/pipelines/report", new ReportHandler());
    server.createContext("/api/pipelines/", new PipelineRouter());
    server.createContext("/metrics", new MetricsHandler());

    server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> {
      Thread t = new Thread(() -> {
        MDC.put("source", "system");
        try {
          r.run();
        } finally {
          MDC.clear();
        }
      });
      t.setDaemon(false);
      return t;
    }));
    server.start();
    MDC.put("source", "system");
    logger.info("Server started on 8080");
  }
}
