package fteam.engine;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Runs pipeline jobs inside Docker containers. */
public class DockerJobExecutor {

  private static final Logger logger = LoggerFactory.getLogger(DockerJobExecutor.class);
  private final DockerClient docker;

  /** Creates a Docker client bound to the local engine. */
  public DockerJobExecutor() {
    this.docker = DockerClientBuilder.getInstance().build();
  }

  /**
   * Executes a job inside a Docker container and returns the resulting exit code.
   *
   * @param image container image to run
   * @param scripts shell commands to execute in order
   * @param repoDir repository directory mounted into the container
   * @return container exit code
   * @throws Exception when container creation or execution fails
   */
  public int runJob(String image, List<String> scripts, File repoDir) throws Exception {
    if (image == null || image.isBlank()) {
      throw new IllegalArgumentException("Job image is required for Docker execution");
    }
    if (scripts == null || scripts.isEmpty()) {
      return 0;
    }

    // 1. pull image (fallback to local image if pull fails)
    try {
      docker.pullImageCmd(image).start().awaitCompletion();
    } catch (Exception e) {
      logger.warn("Image pull failed ({}), trying local image", e.getMessage());
    }

    // 2. merge scripts into one shell command
    String joined = String.join(" && ", scripts);

    // 3. mount repo into container
    String hostRepo = repoDir.getAbsolutePath();
    Volume workDir = new Volume("/workspace");

    HostConfig hostConfig = HostConfig.newHostConfig().withBinds(new Bind(hostRepo, workDir));

    // 4. create a long-running container
    CreateContainerResponse container =
        docker
            .createContainerCmd(image)
            .withHostConfig(hostConfig)
            .withWorkingDir("/workspace")
            .withCmd("sh", "-c", "while true; do sleep 1; done")
            .exec();

    String containerId = container.getId();

    try {
      // 5. start container
      docker.startContainerCmd(containerId).exec();

      // 6. exec actual script
      var execCreate =
          docker
              .execCreateCmd(containerId)
              .withAttachStdout(true)
              .withAttachStderr(true)
              .withCmd("sh", "-c", joined)
              .exec();

      // 7. stream container logs via SLF4J with source=container
      Map<String, String> parentMdc = MDC.getCopyOfContextMap();
      docker
          .execStartCmd(execCreate.getId())
          .exec(
              new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                  if (frame != null && frame.getPayload() != null) {
                    String line = new String(frame.getPayload()).stripTrailing();
                    if (!line.isEmpty()) {
                      if (parentMdc != null) {
                        MDC.setContextMap(parentMdc);
                      }
                      MDC.put("source", "container");
                      logger.info("{}", line);
                      MDC.put("source", "system");
                    }
                  }
                }
              })
          .awaitCompletion();

      // 8. inspect exit code
      Integer exitCode = docker.inspectExecCmd(execCreate.getId()).exec().getExitCode();
      return exitCode == null ? 1 : exitCode;

    } finally {
      // 9. cleanup
      try {
        // docker.removeContainerCmd(containerId).withForce(true).exec();
      } catch (Exception ignore) {
        logger.debug("Container cleanup was skipped for {}", containerId, ignore);
      }
    }
  }
}
