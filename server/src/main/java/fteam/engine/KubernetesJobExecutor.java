package fteam.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1JobStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimSpec;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.openapi.models.V1VolumeResourceRequirements;
import io.kubernetes.client.util.Config;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Runs pipeline jobs as Kubernetes Jobs when the server is executing inside a cluster. */
public class KubernetesJobExecutor {

  private static final Logger logger = LoggerFactory.getLogger(KubernetesJobExecutor.class);
  private static final String NAMESPACE = "default";
  private final BatchV1Api batchApi;
  private final CoreV1Api coreApi;

  /**
   * Creates Kubernetes API clients using the current cluster or local kubeconfig context.
   *
   * @throws Exception when the Kubernetes client cannot be initialized
   */
  public KubernetesJobExecutor() throws Exception {
    ApiClient client = Config.defaultClient();
    Configuration.setDefaultApiClient(client);
    this.batchApi = new BatchV1Api(client);
    this.coreApi = new CoreV1Api(client);
  }

  /**
   * Check if running inside a k8s cluster.
   *
   * @return {@code true} when an in-cluster service account token is available
   */
  public static boolean isInCluster() {
    return new File("/var/run/secrets/kubernetes.io/serviceaccount/token").exists();
  }

  /**
   * Run a pipeline job as a k8s Job.
   *
   * @param image container image (e.g., alpine:3.21)
   * @param scripts shell commands to execute
   * @param pvcName PVC name for workspace mount
   * @param jobLabel unique label for this job (e.g., "compile")
   * @return exit code (0 = success)
   * @throws Exception when the job cannot be created, monitored, or cleaned up
   */
  public int runJob(String image, List<String> scripts, String pvcName, String jobLabel)
      throws Exception {
    if (image == null || image.isBlank()) {
      throw new IllegalArgumentException("Job image is required");
    }
    if (scripts == null || scripts.isEmpty()) {
      return 0;
    }

    String joined = String.join(" && ", scripts);
    String jobName = "cicd-job-" + jobLabel + "-" + System.currentTimeMillis();

    V1Job job =
        new V1Job()
            .apiVersion("batch/v1")
            .kind("Job")
            .metadata(
                new V1ObjectMeta()
                    .name(jobName)
                    .namespace(NAMESPACE)
                    .labels(Map.of("cicd-job", jobLabel)))
            .spec(
                new V1JobSpec()
                    .backoffLimit(0)
                    .template(
                        new V1PodTemplateSpec()
                            .spec(
                                new V1PodSpec()
                                    .restartPolicy("Never")
                                    .containers(
                                        List.of(
                                            new V1Container()
                                                .name("job")
                                                .image(image)
                                                .command(List.of("sh", "-c", joined))
                                                .workingDir("/workspace")
                                                .volumeMounts(
                                                    List.of(
                                                        new V1VolumeMount()
                                                            .name("workspace")
                                                            .mountPath("/workspace")))))
                                    .volumes(
                                        List.of(
                                            new V1Volume()
                                                .name("workspace")
                                                .persistentVolumeClaim(
                                                    new V1PersistentVolumeClaimVolumeSource()
                                                        .claimName(pvcName)))))));

    try {
      logger.info("Creating k8s job: {}", jobName);
      batchApi.createNamespacedJob(NAMESPACE, job).execute();

      int exitCode = waitForJob(jobName);
      logger.info("K8s job {} finished with exit code: {}", jobName, exitCode);

      captureJobPodLogs(jobName);

      return exitCode;

    } finally {
      try {
        batchApi.deleteNamespacedJob(jobName, NAMESPACE).propagationPolicy("Foreground").execute();
        logger.info("Deleted k8s job: {}", jobName);
      } catch (ApiException ignore) {
        logger.debug("Failed to delete k8s job {}", jobName, ignore);
      }
    }
  }

  /**
   * Create a PVC for workspace sharing between pipeline jobs.
   *
   * @param runId run identifier used to derive the PVC name
   * @return created PVC name
   * @throws Exception when PVC creation fails
   */
  public String createWorkspacePvc(String runId) throws Exception {
    String pvcName = "workspace-" + runId;

    V1PersistentVolumeClaim pvc =
        new V1PersistentVolumeClaim()
            .apiVersion("v1")
            .kind("PersistentVolumeClaim")
            .metadata(new V1ObjectMeta().name(pvcName).namespace(NAMESPACE))
            .spec(
                new V1PersistentVolumeClaimSpec()
                    .accessModes(List.of("ReadWriteOnce"))
                    .resources(
                        new V1VolumeResourceRequirements()
                            .requests(
                                Map.of(
                                    "storage",
                                    new io.kubernetes.client.custom.Quantity("100Mi")))));

    coreApi.createNamespacedPersistentVolumeClaim(NAMESPACE, pvc).execute();
    return pvcName;
  }

  /**
   * Delete workspace PVC after pipeline run finishes.
   *
   * @param pvcName PVC name to delete
   */
  public void deleteWorkspacePvc(String pvcName) {
    try {
      coreApi.deleteNamespacedPersistentVolumeClaim(pvcName, NAMESPACE).execute();
    } catch (ApiException ignore) {
      logger.debug("Failed to delete PVC {}", pvcName, ignore);
    }
  }

  /** Capture Pod logs from a completed k8s Job before deletion. */
  private void captureJobPodLogs(String jobName) {
    logger.info("Capturing container logs for k8s job: {}", jobName);
    try {
      List<String> podNames = listPodNamesRaw(jobName);
      logger.info("Found {} pod(s) for job {}", podNames.size(), jobName);
      MDC.put("source", "container");
      for (String podName : podNames) {
        try {
          String logs = coreApi.readNamespacedPodLog(podName, NAMESPACE).execute();
          if (logs != null && !logs.isBlank()) {
            for (String line : logs.split("\n")) {
              logger.info("{}", line);
            }
          }
        } catch (Exception e) {
          logger.warn("Failed to read logs from pod {}: {}", podName, e.getMessage());
        }
      }
      MDC.put("source", "system");
    } catch (Exception e) {
      logger.warn(
          "Failed to capture pod logs for job {}: {} - {}",
          jobName,
          e.getClass().getSimpleName(),
          e.getMessage());
    }
  }

  /**
   * List pod names for a Job using raw HTTP to avoid model deserialization issues with newer K8s
   * API versions.
   */
  private List<String> listPodNamesRaw(String jobName) throws Exception {
    io.kubernetes.client.openapi.ApiClient client = coreApi.getApiClient();
    String selector = URLEncoder.encode("job-name=" + jobName, StandardCharsets.UTF_8);
    String url =
        client.getBasePath()
            + "/api/v1/namespaces/"
            + NAMESPACE
            + "/pods?labelSelector="
            + selector;
    okhttp3.Request request = new okhttp3.Request.Builder().url(url).get().build();
    try (okhttp3.Response response = client.getHttpClient().newCall(request).execute()) {
      String body = response.body().string();
      JsonObject json = JsonParser.parseString(body).getAsJsonObject();
      JsonArray items = json.getAsJsonArray("items");
      List<String> names = new ArrayList<>();
      for (JsonElement item : items) {
        String name = item.getAsJsonObject().getAsJsonObject("metadata").get("name").getAsString();
        names.add(name);
      }
      return names;
    }
  }

  /** Poll k8s Job until it completes. Return exit code. */
  private int waitForJob(String jobName) throws Exception {
    while (true) {
      V1Job job = batchApi.readNamespacedJob(jobName, NAMESPACE).execute();
      V1JobStatus status = job.getStatus();

      if (status != null) {
        if (status.getSucceeded() != null && status.getSucceeded() > 0) {
          return 0;
        }
        if (status.getFailed() != null && status.getFailed() > 0) {
          return 1;
        }
      }

      Thread.sleep(500);
    }
  }
}
