# Week 9 Design: Observability Extension

## Overview

This document describes the observability architecture added to the CI/CD system, covering metrics, logs, and traces for both system services and pipeline job containers.

## Architecture

```
                                ┌──────────────────────────┐
                                │     Grafana (:3000)      │
                                │  queries all 3 backends  │
                                └──┬───────┬───────┬───────┘
                                   │       │       │
                           ┌───────▼──┐ ┌──▼────┐ ┌▼──────┐
                           │Prometheus│ │ Loki  │ │ Tempo │
                           │  (:9090) │ │(:3100)│ │(:3200)│
                           └───┬──────┘ └──▲────┘ └▲──────┘
                               │ scrape     │       │
                      /metrics │        ┌───┴───────┴───┐
                           ┌───▼────────┤  OTel Collector│
                           │            │  (:4317 gRPC) │
                           │            └───▲───────────┘
                           │                │ OTLP (logs + traces)
                      ┌────▼────────────────┴───┐
                      │      CI/CD Server       │
                      │        (:8080)          │
                      └─────────────────────────┘
```

## Data Flow

### Metrics (Pull Model)
- Server exposes `/metrics` endpoint using Prometheus Java `simpleclient`
- Prometheus scrapes this endpoint every 15 seconds
- 5 metrics: `cicd_pipeline_runs_total`, `cicd_pipeline_duration_seconds`, `cicd_stage_duration_seconds`, `cicd_job_duration_seconds`, `cicd_job_runs_total`

### Logs (Push Model)
- Server uses SLF4J + Logback for structured JSON logging
- OpenTelemetry logback appender pushes logs via OTLP gRPC to OTel Collector
- OTel Collector forwards logs to Loki via OTLP HTTP
- MDC provides context fields: `pipeline`, `run_no`, `stage`, `job`, `source`
- `source=system` for server logs, `source=container` for job container stdout/stderr

### Traces (Push Model)
- Server uses OpenTelemetry SDK with OTLP gRPC exporter
- Span hierarchy: pipeline (root) → stage (child) → job (child)
- Root span attributes: `pipeline`, `run_no`
- OTel Context propagated across `CachedThreadPool` threads via `Context.makeCurrent()`
- MDC propagated via `MDC.getCopyOfContextMap()` in thread wrapper
- OTel Collector forwards traces to Tempo

## Technology Choices

| Component | Technology | Reason |
|-----------|-----------|--------|
| Metrics client | Prometheus simpleclient 0.16.0 | Direct Prometheus format, no OTel overhead for metrics |
| Logging framework | Logback 1.4.14 + Logstash encoder 7.4 | JSON structured output, MDC support |
| Log export | OTel logback appender 2.9.0-alpha | Pushes logs to OTel Collector via OTLP |
| Tracing SDK | OpenTelemetry SDK 1.43.0 | Industry standard, native Tempo integration |
| Log/Trace routing | OTel Collector (contrib) | Single ingestion point, routes to Loki + Tempo |
| Metrics backend | Prometheus | Pull-based, native Grafana integration |
| Logs backend | Loki | Log aggregation, native Grafana integration |
| Traces backend | Tempo 2.6.1 | Trace storage, native Grafana integration |
| Visualization | Grafana | Unified dashboards for all 3 pillars |

## Thread Context Propagation

`JobScheduler` uses `Executors.newCachedThreadPool()`. Both MDC and OTel Context are `ThreadLocal`-based and do not auto-propagate to child threads.

Solution in `JobScheduler.executeLevel()`:
```java
Map<String, String> parentMdc = MDC.getCopyOfContextMap();
Context parentContext = Context.current();

executorService.submit(() -> {
    if (parentMdc != null) MDC.setContextMap(parentMdc);
    try (Scope ignored = parentContext.makeCurrent()) {
        job.run();
    } finally {
        MDC.clear();
    }
});
```

## Container Log Capture

- **Docker mode**: Custom `ExecStartResultCallback` captures container stdout/stderr, logs via SLF4J with `source=container` MDC
- **K8s mode**: `CoreV1Api.readNamespacedPodLog()` reads pod logs before job deletion. Uses raw HTTP + Gson for pod name listing to avoid K8s client model deserialization issues with newer API versions

## Deployment

### Local (docker-compose)
- `docker-compose.yml` starts all 7 services
- Config files in `observability/` directory mounted as volumes

### Remote (Helm)
- Helm chart in `helm/cicd-chart/` includes all observability components
- PVCs for Prometheus, Loki, Tempo, Grafana ensure data persistence
- Dashboards provisioned via ConfigMap from `helm/cicd-chart/dashboards/`
- Server deployment includes Prometheus scrape annotations and OTel endpoint env var

## Dashboards

All 4 dashboards provisioned from JSON config files:

| Dashboard | Data Source | Purpose |
|-----------|-----------|---------|
| Pipeline Overview | Prometheus + Loki | Run counts, duration trend, recent runs table with trace_id link |
| Stage & Job Breakdown | Loki | Per-stage/job duration and status, filtered by pipeline + run_no |
| Logs Viewer | Loki | Filter by pipeline/run_no/stage/job/source |
| Trace Explorer | Tempo | Recent traces, click to view span hierarchy |

## Report Integration

`cicd report --run n` includes `traceId` field. The trace ID is stored in MongoDB by `DataStoreAgent.setTraceId()` when the pipeline root span is created. Pipeline Overview dashboard's Recent Runs table shows trace_id as a clickable link to Tempo.
