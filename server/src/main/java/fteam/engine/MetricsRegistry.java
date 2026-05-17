package fteam.engine;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;

/**
 * Central registry for Prometheus metrics exposed by the server.
 */
public final class MetricsRegistry {

  private static final MetricsRegistry INSTANCE = new MetricsRegistry();
  private static final String LABEL_PIPELINE = "pipeline";
  private static final String LABEL_STATUS = "status";
  private static final String LABEL_STAGE = "stage";
  private static final String LABEL_JOB = "job";

  /**
   * Returns the singleton metrics registry.
   *
   * @return shared registry instance
   */
  public static MetricsRegistry getInstance() {
    return INSTANCE;
  }

  private final CollectorRegistry registry;

  private final Counter pipelineRunsTotal;
  private final Histogram pipelineDurationSeconds;
  private final Histogram stageDurationSeconds;
  private final Histogram jobDurationSeconds;
  private final Counter jobRunsTotal;

  private MetricsRegistry() {
    this.registry = CollectorRegistry.defaultRegistry;
    DefaultExports.initialize();

    pipelineRunsTotal = Counter.build()
        .name("cicd_pipeline_runs_total")
        .help("Total number of pipeline runs")
        .labelNames(LABEL_PIPELINE, LABEL_STATUS)
        .register(registry);

    pipelineDurationSeconds = Histogram.build()
        .name("cicd_pipeline_duration_seconds")
        .help("Pipeline execution duration in seconds")
        .labelNames(LABEL_PIPELINE)
        .buckets(1, 5, 10, 30, 60, 120, 300, 600)
        .register(registry);

    stageDurationSeconds = Histogram.build()
        .name("cicd_stage_duration_seconds")
        .help("Stage execution duration in seconds")
        .labelNames(LABEL_PIPELINE, LABEL_STAGE)
        .buckets(1, 5, 10, 30, 60, 120, 300)
        .register(registry);

    jobDurationSeconds = Histogram.build()
        .name("cicd_job_duration_seconds")
        .help("Job execution duration in seconds")
        .labelNames(LABEL_PIPELINE, LABEL_STAGE, LABEL_JOB)
        .buckets(0.5, 1, 5, 10, 30, 60, 120)
        .register(registry);

    jobRunsTotal = Counter.build()
        .name("cicd_job_runs_total")
        .help("Total number of job runs")
        .labelNames(LABEL_PIPELINE, LABEL_STAGE, LABEL_JOB, LABEL_STATUS)
        .register(registry);
  }

  /**
   * Returns the underlying Prometheus collector registry.
   *
   * @return collector registry
   */
  public CollectorRegistry getRegistry() {
    return registry;
  }

  /**
   * Records one pipeline run and its duration.
   *
   * @param pipeline pipeline name
   * @param status final pipeline status
   * @param durationSeconds total run duration in seconds
   */
  public void recordPipelineRun(String pipeline, String status, double durationSeconds) {
    pipelineRunsTotal.labels(pipeline, status).inc();
    pipelineDurationSeconds.labels(pipeline).observe(durationSeconds);
  }

  /**
   * Records one completed stage duration.
   *
   * @param pipeline pipeline name
   * @param stage stage name
   * @param durationSeconds stage duration in seconds
   */
  public void recordStageDuration(String pipeline, String stage, double durationSeconds) {
    stageDurationSeconds.labels(pipeline, stage).observe(durationSeconds);
  }

  /**
   * Records one completed job run and its duration.
   *
   * @param pipeline pipeline name
   * @param stage stage name
   * @param job job name
   * @param status final job status
   * @param durationSeconds job duration in seconds
   */
  public void recordJobRun(String pipeline, String stage, String job,
      String status, double durationSeconds) {
    jobRunsTotal.labels(pipeline, stage, job, status).inc();
    jobDurationSeconds.labels(pipeline, stage, job).observe(durationSeconds);
  }
}
