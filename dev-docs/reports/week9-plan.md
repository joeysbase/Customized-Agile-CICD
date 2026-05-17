# Week 9 Plan — Observability Extension

## Goal

Integrate a full observability solution (metrics, logs, traces) into the CI/CD system, covering both CI/CD services and pipeline job containers. All components must be open source and free.

Requirement spec: https://neu-seattle.gitlab.io/devops/sp26/web/main/project/observability.html

---

## Technology Stack

| Role | Component | Purpose |
|------|-----------|---------|
| Metrics | Prometheus | Scrapes `/metrics` endpoint (pull model); stores time-series data |
| Logs | Loki | Log aggregation backend; native Grafana integration |
| Traces | Tempo | Distributed trace backend; native Grafana integration |
| Collection | OpenTelemetry Collector | Receives logs + traces from server; exports to Loki and Tempo |
| Visualization | Grafana | Unified dashboard for metrics, logs, and traces |

### Architecture

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

Key: Prometheus **pulls** metrics from server `/metrics`. Server **pushes** logs and traces to OTel Collector via OTLP. OTel Collector forwards logs to Loki and traces to Tempo.

---

## Current Codebase State (reference for dev/test sessions)

### Source File Paths

**Server engine** (`server/src/main/java/fteam/engine/`):
- `RunWorker.java` (220 lines) — pipeline orchestrator, main instrumentation target
- `Job.java` (294 lines) — job execution unit, has `startTime`/`endTime`/`status`
- `JobScheduler.java` (226 lines) — DAG scheduling, uses `CachedThreadPool`
- `DockerJobExecutor.java` (82 lines) — runs jobs as Docker containers
- `KubernetesJobExecutor.java` (152 lines) — runs jobs as k8s Jobs
- `DataStoreAgent.java` (154 lines) — MongoDB persistence
- `WorkerManager.java` (132 lines) — worker lifecycle
- `PipelineConfig.java` (456 lines) — YAML parsing
- `ReportWorker.java`, `ReportService.java` — report generation
- `Worker.java` (37 lines) — base abstraction

**Server HTTP** (`server/src/main/java/fteam/server/`):
- `ServerMain.java` (11 lines) — entry point
- `ServerAgent.java` (29 lines) — HTTP server bootstrap on :8080
- `handler/RunHandler.java` (181 lines)
- `handler/ReportHandler.java` (245 lines)
- `handler/VerifyHandler.java`, `handler/DryrunHandler.java`, `handler/PipelineRouter.java`

**Client** (`client/src/main/java/fteam/cli_client/`):
- `ReportSubcommand.java` — needs trace-id output
- `RequestAgent.java` — HTTP client, reads `CICD_SERVER_URL` env var

**Build**: `server/build.gradle` — Java 21, main class `fteam.server.ServerMain`

### Current State Summary

| Aspect | Status |
|--------|--------|
| Logging | `System.out.println` only, no framework, no structured output |
| Metrics | No `/metrics` endpoint, no Prometheus dependency |
| Tracing | No OpenTelemetry, no trace context anywhere |
| Docker Compose | **Does not exist** — only `start-system.sh` for MongoDB |
| Thread model | `CachedThreadPool` in `JobScheduler`, no context propagation |
| K8s Job logs | `KubernetesJobExecutor` does **not** capture Pod logs |
| Docker Job logs | `DockerJobExecutor` prints to `System.out` via callback |

---

## Issue Breakdown

### [Issue #111](https://github.com/CS7580-SEA-SP26/f-team/issues/111): [Infra] Add docker-compose with observability stack

**Weight: L** | **Est: 5-6h**

Add `docker-compose.yml` integrating all services (currently no compose file exists — only `start-system.sh` that manually starts MongoDB).

**Components to include:**
- CI/CD server (existing `Dockerfile`)
- MongoDB 7
- Prometheus (image: `prom/prometheus`)
- Loki (image: `grafana/loki`)
- Tempo (image: `grafana/tempo`)
- OpenTelemetry Collector (image: `otel/opentelemetry-collector-contrib`)
- Grafana (image: `grafana/grafana`)

**Configuration files to create:**
- `observability/prometheus/prometheus.yml` — scrape config targeting `server:8080/metrics`
- `observability/otel-collector/otel-collector-config.yaml` — OTLP receiver, Loki + Tempo exporters
- `observability/loki/loki-config.yaml` — storage and retention
- `observability/tempo/tempo-config.yaml` — storage and retention
- `observability/grafana/provisioning/datasources/datasources.yaml` — Prometheus, Loki, Tempo data sources

**Constraint:** `docker-compose up` must start everything — no separate manual steps allowed.

**Acceptance criteria:**
- [ ] `docker-compose up -d` starts all 7 services without error
- [ ] Grafana accessible at `http://localhost:3000`
- [ ] Prometheus accessible at `http://localhost:9090`, targets page shows server as UP
- [ ] All config files committed to repo (no manual Grafana UI setup)

---

### [Issue #112](https://github.com/CS7580-SEA-SP26/f-team/issues/112): [Server] Add Prometheus metrics endpoint with 5 required metrics

**Weight: M** | **Est: 5-6h**

**Dependencies to add to `server/build.gradle`:**
```
implementation 'io.prometheus:simpleclient:0.16.0'
implementation 'io.prometheus:simpleclient_common:0.16.0'
implementation 'io.prometheus:simpleclient_hotspot:0.16.0'
```

**Register `/metrics` handler** in `ServerAgent.java` (currently 29 lines, has 5 handlers on lines 19-23).

**Required metrics with labels:**

| Metric | Type | Required Labels | Where to instrument |
|--------|------|-----------------|---------------------|
| `cicd_pipeline_runs_total` | Counter | `pipeline`, `status` | `RunWorker` — on pipeline completion |
| `cicd_pipeline_duration_seconds` | Histogram | `pipeline` | `RunWorker` — end-to-end time |
| `cicd_stage_duration_seconds` | Histogram | `pipeline`, `stage` | `JobScheduler.executeStage()` |
| `cicd_job_duration_seconds` | Histogram | `pipeline`, `stage`, `job` | `Job.run()` |
| `cicd_job_runs_total` | Counter | `pipeline`, `stage`, `job`, `status` | `Job.run()` — on job completion |

**Files to modify:**
- `server/build.gradle` — add Prometheus client deps
- `server/src/main/java/fteam/server/ServerAgent.java` — register `/metrics` handler
- `server/src/main/java/fteam/engine/RunWorker.java` — pipeline counter + histogram
- `server/src/main/java/fteam/engine/JobScheduler.java` — stage histogram
- `server/src/main/java/fteam/engine/Job.java` — job counter + histogram

**Note:** `Job` already has `startTime`/`endTime`/`status` fields — data source is ready. Duration = `endTime - startTime`.

**Acceptance criteria:**
- [ ] `GET /metrics` returns Prometheus text format
- [ ] After running a pipeline, all 5 metrics appear with correct labels
- [ ] Prometheus can scrape the endpoint (visible in Prometheus targets)

---

### [Issue #113](https://github.com/CS7580-SEA-SP26/f-team/issues/113): [Server] Replace System.out with SLF4J structured JSON logging

**Weight: M** | **Est: 5-6h**

**Dependencies to add to `server/build.gradle`:**
```
implementation 'ch.qos.logback:logback-classic:1.4.14'
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```
(SLF4J API is transitively included by logback-classic)

**Tasks:**
1. Replace all `System.out.println` calls with SLF4J logger across ~10 files
2. Create `server/src/main/resources/logback.xml` with JSON encoder
3. Each log entry must include: `timestamp` (ISO 8601), `level`, `service` ("server"), `message`
4. Add MDC context (`pipeline`, `run_no`, `stage`, `job`) at execution entry points
5. Handle MDC propagation: `JobScheduler` spawns threads via `CachedThreadPool` — MDC does NOT auto-propagate. Must copy MDC map before submitting tasks and restore it in the worker thread.

**Files to modify:**
- `server/build.gradle`
- `server/src/main/resources/logback.xml` (new file)
- `server/src/main/java/fteam/server/ServerAgent.java` — "Server started" log
- `server/src/main/java/fteam/server/handler/RunHandler.java` — "RUN query=" log
- `server/src/main/java/fteam/engine/RunWorker.java` — pipeline lifecycle logs + set MDC
- `server/src/main/java/fteam/engine/Job.java` — job lifecycle logs
- `server/src/main/java/fteam/engine/JobScheduler.java` — MDC propagation to threads
- `server/src/main/java/fteam/engine/DockerJobExecutor.java` — image pull warning log
- `server/src/main/java/fteam/engine/KubernetesJobExecutor.java` — add execution logs (currently silent)
- Other files with `System.out.println`

**Acceptance criteria:**
- [ ] All server output is JSON-formatted structured logs
- [ ] Each log entry has `timestamp`, `level`, `service`, `message` fields
- [ ] Logs during pipeline execution include `pipeline`, `run_no` context
- [ ] Logs during job execution include `stage`, `job` context
- [ ] Logs are queryable in Loki via Grafana

---

### [Issue #114](https://github.com/CS7580-SEA-SP26/f-team/issues/114): [Server] Add OpenTelemetry tracing for pipeline/stage/job spans

**Weight: L** | **Est: 8-10h** (most complex issue)

**Dependencies to add to `server/build.gradle`:**
```
implementation platform('io.opentelemetry:opentelemetry-bom:1.40.0')
implementation 'io.opentelemetry:opentelemetry-api'
implementation 'io.opentelemetry:opentelemetry-sdk'
implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
implementation 'io.opentelemetry:opentelemetry-sdk-trace'
```

**Span hierarchy:**
```
Pipeline Span (root)        — created in RunWorker.run()
  └── Stage Span (child)    — created in JobScheduler.executeStage()
        └── Job Span (child) — created in Job.run()
```

**Tasks:**
1. Initialize `SdkTracerProvider` with OTLP exporter targeting OTel Collector (`http://otel-collector:4317`) in `ServerMain.java` or a new `TelemetryConfig.java`
2. Create root span in `RunWorker.run()` with attributes: `pipeline`, `run_no`
3. Create stage child spans in `JobScheduler.executeStage()`
4. Create job child spans in `Job.run()`
5. **Solve thread context propagation:** `JobScheduler` uses `Executors.newCachedThreadPool()` — OTel context is `ThreadLocal`-based and NOT inherited. Must wrap `Runnable` submissions with `Context.current().wrap()` so child spans correctly parent to stage spans.
6. Extract `trace-id` from root span and persist to MongoDB via `DataStoreAgent` (add `traceId` field to run document)

**Files to modify:**
- `server/build.gradle`
- `server/src/main/java/fteam/server/ServerMain.java` — OTel SDK init
- `server/src/main/java/fteam/engine/RunWorker.java` — root span
- `server/src/main/java/fteam/engine/JobScheduler.java` — stage spans + context wrapping
- `server/src/main/java/fteam/engine/Job.java` — job spans
- `server/src/main/java/fteam/engine/DataStoreAgent.java` — store `traceId` in run document

**Key risk:** Thread context propagation across `CachedThreadPool`. If not wrapped correctly, job spans will be orphaned (no parent).

**Acceptance criteria:**
- [ ] Running a pipeline produces a trace visible in Tempo
- [ ] Trace shows correct hierarchy: pipeline → stage → job
- [ ] Root span has `pipeline` and `run_no` attributes
- [ ] `traceId` is stored in MongoDB run document
- [ ] Parallel jobs within a stage each have their own span under the same stage parent

---

### [Issue #115](https://github.com/CS7580-SEA-SP26/f-team/issues/115): [Server] Capture and forward job container logs (Docker + K8s)

**Weight: M** | **Est: 4-5h** | **Depends on: #111, #113**

**Requirement:** all stdout/stderr from job containers must be collected, labeled with `pipeline`, `run_no`, `stage`, `job`, and forwarded to Loki. No instrumentation inside job containers is required.

**Current state:**
- `DockerJobExecutor.java` (82 lines): logs go to `System.out`/`System.err` via `ExecStartResultCallback` — not captured as structured logs
- `KubernetesJobExecutor.java` (152 lines): **no log capture at all** — Pod logs are lost after deletion

**Tasks:**
- **Docker mode:** In `DockerJobExecutor`, capture container stdout/stderr output and emit as structured log entries via SLF4J with MDC set to `pipeline`, `run_no`, `stage`, `job`
- **K8s mode:** In `KubernetesJobExecutor`, call `CoreV1Api.readNamespacedPodLog()` **before** deleting the Pod/Job, emit each line as structured log entry with same MDC labels
- Distinguish container logs from system logs (add a `source: "container"` field vs `source: "system"`)

**Files to modify:**
- `server/src/main/java/fteam/engine/DockerJobExecutor.java`
- `server/src/main/java/fteam/engine/KubernetesJobExecutor.java`

**Risk:** K8s Jobs are short-lived — must read logs before Pod deletion. Add retry/wait logic if Pod not yet in terminal state.

**Acceptance criteria:**
- [ ] After running a pipeline, job container stdout/stderr appears in Loki
- [ ] Each log entry is labeled with `pipeline`, `run_no`, `stage`, `job`
- [ ] Container logs and system logs are distinguishable (different `source` label)
- [ ] Works in both Docker and K8s execution modes

---

### [Issue #116](https://github.com/CS7580-SEA-SP26/f-team/issues/116): [Server+CLI] Add trace-id to report output

**Weight: S** | **Est: 2h** | **Depends on: #114**

When `cicd report --run n` is called, include `trace-id` at the pipeline level:

```
pipeline:
  name: default
  run-no: 1
  status: success
  trace-id: 4bf92f3577b34da6a3ce929d0e0e4736
  start: 2025-08-29T16:17:52-07:00
  end: 2025-08-29T16:24:32-07:00
```

**Tasks:**
- Server: `ReportService.java` / `ReportHandler.java` — read `traceId` from MongoDB run document, include in response JSON
- Client: `ReportSubcommand.java` — parse and display `trace-id` field

**Files to modify:**
- `server/src/main/java/fteam/engine/ReportService.java` or `ReportWorker.java`
- `server/src/main/java/fteam/server/handler/ReportHandler.java`
- `client/src/main/java/fteam/cli_client/ReportSubcommand.java`

**Acceptance criteria:**
- [ ] `cicd report --pipeline X --run 1` output includes `trace-id` field
- [ ] `trace-id` value matches the trace visible in Tempo for that run
- [ ] `trace-id` only appears when querying a specific run (not in run list)

---

### [Issue #117](https://github.com/CS7580-SEA-SP26/f-team/issues/117): [Infra] Provision 4 Grafana dashboards as code

**Weight: M** | **Est: 6-8h** | **Depends on: #111, #112, #113, #114**

All dashboards must be provisioned from JSON config files — manual UI creation does not count.

| Dashboard | Data Source | Key Panels |
|-----------|------------|------------|
| Pipeline Overview | Prometheus | Runs by status (bar), duration over time (line), recent runs table (name, run#, branch, commit, status, duration) |
| Stage & Job Breakdown | Prometheus | Per-stage duration (bar), per-job duration (bar), job status breakdown (success/fail counts) |
| Logs Viewer | Loki | Filter by `pipeline`, `run_no`, `stage`, `job`; show system + container logs in single view with distinguishable sources |
| Trace Explorer | Tempo | Full span hierarchy (pipeline → stage → job) for a given `run_no`; or direct link from Pipeline Overview to Tempo trace |

**Files to create:**
- `observability/grafana/provisioning/dashboards/dashboard-provider.yaml`
- `observability/grafana/dashboards/pipeline-overview.json`
- `observability/grafana/dashboards/stage-job-breakdown.json`
- `observability/grafana/dashboards/logs-viewer.json`
- `observability/grafana/dashboards/trace-explorer.json`

**Approach:** Design in Grafana UI first → export JSON → commit as provisioned dashboard files.

**Acceptance criteria:**
- [ ] All 4 dashboards appear automatically on `docker-compose up` (no manual import)
- [ ] Pipeline Overview shows run counts, duration trends, and recent runs table
- [ ] Stage & Job Breakdown allows selecting pipeline + run, shows per-stage/job metrics
- [ ] Logs Viewer filters work for pipeline/run/stage/job and shows both system + container logs
- [ ] Trace Explorer displays span hierarchy for a given run_no

---

### [Issue #118](https://github.com/CS7580-SEA-SP26/f-team/issues/118): [k8s] Extend Helm chart with observability stack + PVC persistence

**Weight: L** | **Est: 5-6h** | **Depends on: #111**

Add all observability components to the Helm chart. Data must persist across restarts (PVC required).

**New Helm templates to add under `helm/cicd-chart/templates/`:**
- `prometheus-deployment.yaml`, `prometheus-service.yaml`, `prometheus-pvc.yaml`, `prometheus-configmap.yaml`
- `loki-deployment.yaml`, `loki-service.yaml`, `loki-pvc.yaml`, `loki-configmap.yaml`
- `tempo-deployment.yaml`, `tempo-service.yaml`, `tempo-pvc.yaml`, `tempo-configmap.yaml`
- `otel-collector-deployment.yaml`, `otel-collector-service.yaml`, `otel-collector-configmap.yaml`
- `grafana-deployment.yaml`, `grafana-service.yaml`, `grafana-pvc.yaml`, `grafana-configmap.yaml`

**Extend `values.yaml`** with observability config (images, ports, retention, PVC storage sizes).

**Add Prometheus scrape annotations** to server Deployment template.

**Add OTel Collector endpoint env vars** to server Deployment template (`OTEL_EXPORTER_OTLP_ENDPOINT`).

**Acceptance criteria:**
- [ ] `helm install` deploys all observability components alongside server + MongoDB
- [ ] Grafana accessible via NodePort
- [ ] Observability data survives `kubectl delete pod <prometheus/loki/tempo-pod>` (PVC backed)
- [ ] Server metrics scraped by Prometheus, logs/traces flow through OTel Collector

---

### [Issue #119](https://github.com/CS7580-SEA-SP26/f-team/issues/119): Write observability design doc + week 9 weekly report

**Weight: S** | **Est: 2-3h** | **Depends on: all above**

- Create `dev-docs/design/week9-observability-design.md` — architecture diagrams, component interactions, data flow, design decisions
- Write `dev-docs/reports/weeklies/week9.md` — following template: completed tasks, carry over, what worked, what didn't, design updates

**Acceptance criteria:**
- [ ] Design doc covers architecture, tech stack choices, data flow for metrics/logs/traces
- [ ] Weekly report lists all issues with links, weights, assignees
- [ ] All DONE issues are closed on GitHub with linked PRs

---

## Dependency Graph

```
#111 (docker-compose) ─────────┬──► #115 (container logs)
                                ├──► #117 (dashboards)
                                └──► #118 (Helm)

#112 (metrics) ────────────────┬──► #117 (dashboards)

#113 (structured logs) ────────┬──► #115 (container logs)
                                └──► #117 (dashboards)

#114 (tracing) ────────────────┬──► #116 (trace-id in report)
                                └──► #117 (dashboards)

#115, #116, #117, #118 ───────┬──► #119 (design doc + weekly)
```

**Critical path:** #114 (tracing) → #116 (trace-id) → #119 (docs)

## Branch Strategy

```
main (stable release)
  └── dev (integration branch)
        ├── issue-111 (docker-compose)
        ├── issue-112 (metrics)
        ├── issue-113 (logging)
        ├── issue-114 (tracing)
        ├── issue-115 (container logs)
        ├── issue-116 (trace-id report)
        ├── issue-117 (dashboards)
        ├── issue-118 (Helm)
        └── issue-119 (docs)
```

- **All feature branches are created from `dev`** (not from `main`)
- **All PRs merge back to `dev`**
- Branch naming: `issue-{number}` (e.g., `issue-111`)
- Commit message: `Fix #{number}: description` (e.g., `Fix #111: add docker-compose with observability stack`)
- After all issues are done, tests pass, and weekly report is complete → merge `dev` into `main`

### PR Strategy

Issues are grouped into PRs to minimize merge conflicts (similar to Week 8 strangler fig approach):

| PR | Issues | Branch Chain | Rationale |
|----|--------|-------------|-----------|
| PR 1 | #111 | `issue-111` | Infra foundation — all other issues depend on it |
| PR 2 | #112 → #113 → #114 | `issue-112` → `issue-113` → `issue-114` (chained) | All modify `build.gradle`, `RunWorker`, `JobScheduler`, `Job` — chaining avoids 3-way conflicts |
| PR 3 | #115 → #116 | `issue-115` → `issue-116` (chained) | Both depend on PR2, overlapping files (executors + report) |
| PR 4 | #117 | `issue-117` | Pure Grafana JSON config, no Java code |
| PR 5 | #118 | `issue-118` | Pure Helm templates, no Java code |
| PR 6 | #119 | `issue-119` | Docs only, written last |

**Merge order:** PR1 → PR2 → PR3 → PR4 + PR5 (parallel) → PR6

For chained PRs: create `issue-112` from `dev`, then `issue-113` from `issue-112`, then `issue-114` from `issue-113`. Final branch contains all changes and is merged to `dev` as one PR. Each intermediate issue still gets its own commit(s) with `Fix #{number}:` message.

## Parallelization Strategy

Issues #111, #112, #113, #114 have **no dependencies on each other** and can be developed in parallel.

| Track | Issues | Focus |
|-------|--------|-------|
| Track A: Infrastructure | #111 → #118 | docker-compose, Helm, config files |
| Track B: Server instrumentation | #112 + #113 + #114 → #115 → #116 | Java code changes |
| Track C: Dashboards + docs | #117 → #119 | Grafana JSON, design doc, weekly report |

---

## Time Summary

| Issue | Title | Weight | Est. Time |
|-------|-------|--------|-----------|
| #111 | docker-compose + observability stack | L | 5-6h |
| #112 | Prometheus metrics endpoint | M | 5-6h |
| #113 | SLF4J structured JSON logging | M | 5-6h |
| #114 | OpenTelemetry tracing | L | 8-10h |
| #115 | Job container log capture | M | 4-5h |
| #116 | trace-id in report output | S | 2h |
| #117 | Grafana dashboards as code | M | 6-8h |
| #118 | Helm chart extension | L | 5-6h |
| #119 | Design doc + weekly report | S | 2-3h |
| | **Total** | | **42-52h** |

### By team size

| Team | Estimated duration |
|------|--------------------|
| 1 person | ~1.5-2 weeks |
| 2 people | ~1 week |
| 3 people | ~4-5 days |

---

## Test Strategy (for test session)

### Per-Issue Verification

| Issue | How to test |
|-------|-------------|
| #111 | `docker-compose up -d` → all 7 containers running → Grafana at :3000 → Prometheus at :9090 shows server target UP |
| #112 | `curl http://localhost:8080/metrics` returns Prometheus text format → run a pipeline → curl again → 5 metrics present with correct labels |
| #113 | Run a pipeline → check server stdout is JSON → query Loki in Grafana → filter by `pipeline=X` returns structured logs |
| #114 | Run a pipeline → query Tempo in Grafana → trace exists with pipeline→stage→job hierarchy → check MongoDB `db.pipeline_runs.findOne()` has `traceId` field |
| #115 | Run a pipeline with a job that prints to stdout → query Loki → container logs appear with `pipeline`, `run_no`, `stage`, `job` labels → both Docker and K8s modes |
| #116 | `./cicd report --pipeline X --run 1` output includes `trace-id:` → value matches Tempo trace |
| #117 | `docker-compose up` → Grafana → all 4 dashboards auto-loaded → each dashboard renders panels correctly |
| #118 | `helm install` in Minikube → all pods running → Grafana NodePort accessible → data persists after pod restart |

### End-to-End Smoke Test

1. `docker-compose up -d`
2. `./cicd run --name test --branch main --file .pipelines/allow-failure.yaml`
3. Verify in Grafana:
   - Pipeline Overview dashboard shows the run
   - Stage & Job Breakdown shows per-stage/job timing
   - Logs Viewer shows system + container logs filtered by `pipeline=test`
   - Trace Explorer shows pipeline→stage→job span hierarchy
4. `./cicd report --pipeline test --run 1` — output includes `trace-id`
5. Copy trace-id → paste in Tempo search → same trace found
