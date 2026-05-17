# Week 9


# Completed tasks

| Task | Weight | Assignee |
| ---- | ------ | -------- |
| [Issue #111: [Infra] Add docker-compose with observability stack](https://github.com/CS7580-SEA-SP26/f-team/issues/111) | L | jason-te-sde |
| [Issue #112: [Server] Add Prometheus metrics endpoint with 5 required metrics](https://github.com/CS7580-SEA-SP26/f-team/issues/112) | M | jason-te-sde |
| [Issue #113: [Server] Replace System.out with SLF4J structured JSON logging](https://github.com/CS7580-SEA-SP26/f-team/issues/113) | M | jason-te-sde |
| [Issue #114: [Server] Add OpenTelemetry tracing for pipeline/stage/job spans](https://github.com/CS7580-SEA-SP26/f-team/issues/114) | L | jason-te-sde |
| [Issue #115: [Server] Capture and forward job container logs (Docker + K8s)](https://github.com/CS7580-SEA-SP26/f-team/issues/115) | M | jason-te-sde |
| [Issue #116: [Server+CLI] Add trace-id to report output](https://github.com/CS7580-SEA-SP26/f-team/issues/116) | S | jason-te-sde |
| [Issue #117: [Infra] Provision 4 Grafana dashboards as code](https://github.com/CS7580-SEA-SP26/f-team/issues/117) | M | jason-te-sde |
| [Issue #118: [k8s] Extend Helm chart with observability stack + PVC persistence](https://github.com/CS7580-SEA-SP26/f-team/issues/118) | L | jason-te-sde |
| [Issue #119: [Docs] Write observability design doc + week 9 weekly report](https://github.com/CS7580-SEA-SP26/f-team/issues/119) | S | jason-te-sde |

# Carry over tasks

| Task | Weight | Assignee |
| ---- | ------ | -------- |
|      |        |          |


# What worked this week?

### 1. Chained PR strategy for conflicting files
Issues #112, #113, #114 all modified `build.gradle`, `RunWorker`, `JobScheduler`, and `Job`. Chaining branches (issue-112 → issue-113 → issue-114) and merging as a single PR avoided three-way merge conflicts.

* **Why it worked:** Each issue built on the previous one's changes. The final branch contained all changes in a clean, linear history, and the single PR to dev had no conflicts.

### 2. Separate dev and test roles
Development and testing were done in separate sessions with independent perspectives. The test session provided detailed bug reports with root cause analysis, which made fixes targeted and efficient.

* **Why it worked:** The tester caught issues that the developer missed (e.g., OTel logback appender not installed, MDC not propagating to non-main threads, K8s client deserialization errors). Each fix was driven by concrete test evidence rather than guesswork.

### 3. UI-first dashboard development
After multiple failed attempts at hand-writing Grafana dashboard JSON, switching to designing dashboards in the Grafana UI first and then exporting the JSON was far more effective.

* **Why it worked:** Grafana 12's JSON schema is complex and version-specific. The UI handles all the schema details correctly, and exporting produces JSON that is guaranteed to work when re-provisioned.

# What did not work this week?

### 1. Hand-writing Grafana dashboard JSON
Initially attempted to write all 4 dashboard JSON files by hand without testing in the Grafana UI. This led to 6+ rounds of fixes for issues like wrong datasource variable references, incorrect Loki query syntax, and incompatible panel configurations.

* **Why it didn't work well:** Grafana's provisioned dashboard format differs from its UI export format (e.g., `${DS_PROMETHEUS}` variables don't resolve in provisioned mode). These issues are nearly impossible to debug without the UI.

### 2. OTel dependency version mismatches
The OpenTelemetry logback appender version (2.9.0-alpha) was initially paired with an incompatible BOM version (1.40.0), causing `NoClassDefFoundError` at runtime. Required multiple iterations to align all OTel dependency versions.

* **Why it didn't work well:** The OTel ecosystem has a core BOM and an instrumentation BOM with different version numbers. Using the wrong combination causes runtime class loading failures that aren't caught at compile time.

# Design updates

- [Week 9 Observability Design](../../design/week9-observability-design.md) — Architecture, data flow for metrics/logs/traces, technology choices, thread context propagation, container log capture, deployment (docker-compose + Helm), and dashboard design.
