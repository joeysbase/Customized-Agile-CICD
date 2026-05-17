# Observability Demo

## Setup

```bash
# Build server image into Minikube's Docker daemon
eval $(minikube docker-env)
docker build -t cicd-server:latest .

# Deploy full stack: server + MongoDB + Prometheus + Loki + Tempo + OTel Collector + Grafana
helm install cicd helm/cicd-chart/
kubectl get pods -w   # Wait for all 7 pods Running, then Ctrl+C

# Port-forward server and Grafana
kubectl port-forward svc/cicd-cicd-server-service 8080:8080 &
kubectl port-forward svc/cicd-grafana-service 3000:3000 &
export CICD_SERVER_URL=http://localhost:8080

# Run a pipeline to generate metrics, logs, and traces
./cicd run --file .pipelines/allow-failure.yaml
```

13 jobs across 4 stages. 12 succeed, 1 allowed failure (`coverage-report`). Wait ~15s for telemetry ingestion.

---

## Metrics

All 5 required metrics are exposed at `/metrics` in Prometheus text format.

```bash
curl -s http://localhost:8080/metrics | grep -E "^cicd_"
```

- `cicd_pipeline_runs_total` — pipeline run count by status
- `cicd_pipeline_duration_seconds` — pipeline duration histogram
- `cicd_stage_duration_seconds` — per-stage duration histogram
- `cicd_job_duration_seconds` — per-job duration histogram
- `cicd_job_runs_total` — job run count by status

---

## Dashboard 1: Pipeline Overview

Open http://localhost:3000 → Dashboards → Pipeline Overview. Set time range to Last 1 hour.

- **Pipeline Runs by Status** — bar chart showing run counts grouped by status
- **Pipeline Duration Over Time** — time series showing pipeline duration trend
- **Recent Pipeline Runs** — table with Name, Run#, Branch, Commit, Status, Duration(s), trace_id
- Click `trace_id` link → opens Tempo Explore with full span hierarchy for that run

---

## Dashboard 2: Stage & Job Breakdown

Open Stage & Job Breakdown. Set Pipeline = `sprint6-allowed-failure-demo`, Run No = `1`.

- **Per-Stage Duration** — table showing each stage's status and duration
- **Per-Job Duration** — table showing each job's stage, status, and duration
- **Job Status Breakdown** — pie chart showing success vs failure counts

---

## Dashboard 3: Logs Viewer

Open Logs Viewer. Set Pipeline = `sprint6-allowed-failure-demo`, Run No = `1`.

- Default view shows all logs for this pipeline run
- Set Source = `system` → only CI/CD server logs (Starting job, Stage finished, etc.)
- Set Source = `container` → only job container stdout/stderr (Fetching source code, Compiling, etc.)
- Set Stage = `test`, Job = `coverage-report` → isolate the failed job's logs

System and container logs are visible in a single view with source distinguishable.

---

## Dashboard 4: Trace Explorer

Open Trace Explorer. Table lists all traces from the CI/CD server.

- Click any trace → displays full span hierarchy: pipeline → stage → job with durations
- Root span shows `pipeline` and `run_no` as span attributes
- Parallel jobs appear as concurrent spans under the same stage parent

---

## Report with Trace ID

The `report` command includes `traceId` when querying a specific run.

```bash
./cicd report --pipeline sprint6-allowed-failure-demo --run 1
```

Copy the `traceId` value → go to Pipeline Overview → Recent Runs table → same trace_id matches.

---

## Data Persistence

Observability data persists across pod restarts via PVCs.

```bash
# Verify 4 PVCs are Bound
kubectl get pvc

# Delete Prometheus pod to simulate restart
kubectl delete $(kubectl get pods -o name | grep prometheus | head -1)
kubectl get pods -w   # Wait for replacement pod to be Running
```

Go back to Grafana → Pipeline Overview. Data is still present after pod restart.

---

## Cleanup

```bash
# Stop port-forwards
pkill -f "port-forward.*8080" 2>/dev/null
pkill -f "port-forward.*3000" 2>/dev/null

# Remove all resources
helm uninstall cicd
kubectl delete pvc --all
```
