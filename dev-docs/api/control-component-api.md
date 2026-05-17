# Control Component API

This document describes the HTTP API exposed by the CI/CD control component running on port `8080`.

## Conventions

- Content type for API responses is plain text
- Successful verify/dryrun/run/report responses use YAML-like text
- Errors are returned as plain text strings beginning with `ERROR:`
- Query-parameter GET routes exist mainly for compatibility and CLI usage

## Endpoints

### `POST /api/pipelines/verify`

Purpose:
- validate a pipeline YAML document supplied in the request body

Input:
- request body: raw pipeline YAML text

Success output:

```yaml
verify:
  valid: true|false
  messages:
    - ...
```

Failure cases:
- HTTP `400` when the request body is empty
- HTTP `405` when the method is not allowed

### `GET /api/pipelines/verify?file=<path>`

Purpose:
- validate a pipeline YAML file on disk

Input:
- query param `file`: path to the YAML file

Success output:
- same body shape as `POST /api/pipelines/verify`

Failure cases:
- HTTP `400` if `file` is missing
- HTTP `405` when the method is not allowed

### `POST /api/pipelines/dryrun`

Purpose:
- validate a pipeline YAML document and render the execution plan without running jobs

Input:
- request body: raw pipeline YAML text

Success output:

```yaml
dryrun:
  valid: true|false
  plan:
    - stage: build
      job: compile
```

When the config is invalid:

```yaml
dryrun:
  valid: false
  messages:
    - ...
```

Failure cases:
- HTTP `400` when the request body is empty
- HTTP `405` when the method is not allowed

### `GET /api/pipelines/dryrun?file=<path>`

Purpose:
- load a pipeline file from disk and preview the execution plan

Input:
- query param `file`: path to the YAML file

Success output:
- same body shape as `POST /api/pipelines/dryrun`

Failure cases:
- HTTP `400` if `file` is missing
- HTTP `405` when the method is not allowed

### `POST /api/pipelines/run?repo=<path>&branch=<branch>&commit=<commit>`

Purpose:
- execute a pipeline YAML document against a repository

Input:
- request body: raw pipeline YAML text
- query param `repo`: repository path
- query param `branch`: optional, defaults to `main`
- query param `commit`: optional, defaults to `latest`

Behavior notes:
- outside Kubernetes mode, the handler verifies that the requested branch/commit matches the current repository checkout
- inside Kubernetes mode, the repo handling is relaxed because execution is in-cluster

Success output:

```yaml
run:
  valid: true
  messages:
    - Run-No: 7
    - ...
```

Invalid config output:

```yaml
run:
  valid: false
  messages:
    - ...
```

Failure cases:
- HTTP `400` when `repo` is missing in local mode
- HTTP `400` when the request body is empty
- HTTP `400` when the requested branch or commit does not match the local checkout
- HTTP `500` when the local git check fails
- HTTP `500` when server-side execution fails
- HTTP `405` when the method is not allowed

### `GET /api/pipelines/run?file=<path>&repo=<path>&branch=<branch>&commit=<commit>`

Purpose:
- execute a pipeline file from disk against a repository

Input:
- query param `file`: YAML file path
- query param `repo`: repository path
- query param `branch`: optional, defaults to `main`
- query param `commit`: optional, defaults to `latest`

Success output:
- same body shape as `POST /api/pipelines/run`

Failure cases:
- HTTP `400` when `file` is missing
- HTTP `400` when `repo` is missing
- HTTP `400` for branch/commit mismatch in local mode
- HTTP `500` for git validation or execution failures
- HTTP `405` when the method is not allowed

### `GET /api/pipelines/report?pipeline=<name>&run=<run>&stage=<stage>&job=<job>`

Purpose:
- fetch stored execution results through query parameters

Inputs:
- `pipeline` is required
- `run` is optional
- `stage` requires `run`
- `job` requires `stage`

Success output:
- YAML-like plain text report rendered by `ReportService`

Failure cases:
- HTTP `400` when `pipeline` is missing
- HTTP `400` when `stage` is provided without `run`
- HTTP `400` when `job` is provided without `stage`
- HTTP `400` when `run` is not an integer
- HTTP `500` for report rendering failures
- HTTP `405` when the method is not allowed

### `GET /api/pipelines/{pipeline}/runs/{run}/{stage}/{job}`

Purpose:
- fetch stored execution results through path-based routing

Routing rules:
- `/api/pipelines/{pipeline}/runs`
- `/api/pipelines/{pipeline}/runs/{run}`
- `/api/pipelines/{pipeline}/runs/{run}/{stage}`
- `/api/pipelines/{pipeline}/runs/{run}/{stage}/{job}`

Success output:
- same YAML-like plain text report rendered by `ReportService`

Failure cases:
- HTTP `400` when pipeline name is blank
- HTTP `400` when `run` is not an integer
- HTTP `404` when the path shape is invalid
- HTTP `405` when the method is not allowed
- HTTP `500` for report rendering failures

### `GET /metrics`

Purpose:
- expose Prometheus metrics for scraping

Success output:
- HTTP `200`
- content type: Prometheus text exposition format

Failure cases:
- HTTP `405` when the method is not allowed

## Route Summary

| Route | Method | Purpose |
|---|---|---|
| `/api/pipelines/verify` | `POST` | verify YAML body |
| `/api/pipelines/verify?file=...` | `GET` | verify YAML file |
| `/api/pipelines/dryrun` | `POST` | dry-run YAML body |
| `/api/pipelines/dryrun?file=...` | `GET` | dry-run YAML file |
| `/api/pipelines/run` | `POST` | run YAML body |
| `/api/pipelines/run?file=...` | `GET` | run YAML file |
| `/api/pipelines/report?...` | `GET` | query-based reporting |
| `/api/pipelines/{pipeline}/runs/...` | `GET` | path-based reporting |
| `/metrics` | `GET` | Prometheus scrape endpoint |
