# Feature Status

This file summarizes the current repository-submission status of the project in a grader-facing format.

## Implemented Features

### Core pipeline features

- `verify` validates pipeline YAML files and reports validation messages
- `dryrun` renders the planned execution order without running jobs
- `run` executes jobs in dependency order
- `report` renders stored pipeline, run, stage, and job results

### Execution modes

- local job execution through Docker containers
- Kubernetes job execution through transient batch jobs

### Allow failures

- jobs can be marked with `failures: true`
- allowed-failure jobs are recorded as failed while allowing the pipeline to continue

### Observability

- Prometheus metrics exposed at `/metrics`
- structured logs routed through OpenTelemetry to Loki
- distributed traces routed through OpenTelemetry to Tempo
- Grafana dashboards provisioned as code

### Deployment

- local deployment through Docker Compose
- raw Kubernetes deployment through `k8s/`
- Helm deployment through `helm/cicd-chart/`
- published server image on Docker Hub

### Developer quality gates

- Javadoc warnings cleaned up for the main public API surface
- Google-style Checkstyle configuration added and enforced in CI
- PMD violations cleaned up and enforced in CI
- PR and main branch GitHub Actions workflows added

## Repository Quality Status

### Automated tests and coverage

- automated tests are implemented for both `client` and `server`
- JaCoCo coverage reporting and `jacocoTestCoverageVerification` are wired into Gradle
- the repository now passes `./gradlew test jacocoTestReport jacocoTestCoverageVerification`

### Client distribution

- the repository includes release automation for publishing build artifacts
- the README documents the client packaging and release flow alongside the server image workflow

## Supporting Documentation

- API documentation: [dev-docs/api/control-component-api.md](dev-docs/api/control-component-api.md)
- Datastore documentation: [dev-docs/design/datastore.md](dev-docs/design/datastore.md)
- Main usage guide: [README.md](README.md)
