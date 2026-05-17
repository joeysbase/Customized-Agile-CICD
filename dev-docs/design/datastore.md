# Datastore Design

This document describes the MongoDB persistence model used by the control component.

## Database

- default database name: `cicd`
- configured by environment variable: `MONGO_DB`
- Mongo connection URI configured by: `MONGO_URI`

## Collections

### `pipeline_runs`

Purpose:
- one document per pipeline execution

Important fields:
- `pipeline`
- `runNo`
- `status`
- `startTime`
- `endTime`
- `gitRepo`
- `gitBranch`
- `gitHash`
- `traceId`

Natural key used by application logic:
- `(pipeline, runNo)`

Query intent:
- list all runs for a pipeline
- fetch one run
- attach trace correlation information

### `stage_runs`

Purpose:
- one document per stage within a pipeline run

Important fields:
- `pipeline`
- `runNo`
- `stage`
- `status`
- `startTime`
- `endTime`

Natural key used by application logic:
- `(pipeline, runNo, stage)`

Query intent:
- list stages for a run
- fetch one stage

### `job_runs`

Purpose:
- one document per job within a stage and run

Important fields:
- `pipeline`
- `runNo`
- `stage`
- `job`
- `status`
- `startTime`
- `endTime`
- `errorMessage`
- `failures`

Natural key used by application logic:
- `(pipeline, runNo, stage, job)`

Query intent:
- list jobs for a stage
- fetch one specific job
- distinguish blocking failures from allowed failures

### `counters`

Purpose:
- maintain the next sequential run number for each pipeline

Important fields:
- `_id` = pipeline name
- `seq`

Natural key:
- `_id`

Query intent:
- atomically allocate the next `runNo`

## Relationships

Logical hierarchy:

```text
pipeline_runs
  -> stage_runs
    -> job_runs
```

More specifically:
- one `pipeline_runs` document has many `stage_runs`
- one `stage_runs` document has many `job_runs`
- one `pipeline_runs` document maps to one `counters` row by pipeline name for run number allocation

## Keys Used by the Code

The application consistently queries by:

- run lookup: `pipeline + runNo`
- stage lookup: `pipeline + runNo + stage`
- job lookup: `pipeline + runNo + stage + job`

These compound lookup patterns are visible in `DataStoreAgent` through repeated `Filters.and(eq(...), ...)` queries.

## Indexes

### Current state

The current implementation relies on MongoDB collection defaults and application-level natural keys.

The code does **not** currently create explicit indexes during startup.

### Recommended indexes for production-hardening

If the repository were being hardened further, the most useful indexes would be:

- `pipeline_runs`: compound index on `(pipeline, runNo)`
- `stage_runs`: compound index on `(pipeline, runNo, stage)`
- `job_runs`: compound index on `(pipeline, runNo, stage, job)`
- `counters`: `_id` is already the natural lookup key

These recommended indexes directly match the access patterns in `findRun`, `findStage`, `findJob`, and the related update/upsert methods.

## Run Number Allocation

Run numbers are generated with `findOneAndUpdate(..., $inc: {seq: 1})` against the `counters` collection using:

- upsert enabled
- return document after update

This gives each pipeline a monotonic sequence of run numbers without needing a global counter.

## Report Query Mapping

The reporting layer maps to the datastore as follows:

- pipeline report -> `findRuns(pipeline)`
- run report -> `findRun(pipeline, runNo)` plus `findStages(...)`
- stage report -> `findStage(...)` plus `findJobs(...)`
- job report -> `findJob(...)`

This is why the hierarchy in the user-facing report output mirrors the MongoDB document model so closely.
