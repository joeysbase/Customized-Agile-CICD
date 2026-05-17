# Week 8


# Completed tasks

| Task                                                                                                                                                               | Weight | Assignee     |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|--------------|
| [Issue #95: Migrate verify endpoint to POST /api/pipelines/verify](https://github.com/CS7580-SEA-SP26/f-team/issues/95)                                            | S      | jason-te-sde |
| [Issue #96: Migrate dryrun endpoint to POST /api/pipelines/dryrun](https://github.com/CS7580-SEA-SP26/f-team/issues/96)                                            | S      | jason-te-sde |
| [Issue #97: Migrate run endpoint to POST /api/pipelines/run](https://github.com/CS7580-SEA-SP26/f-team/issues/97)                                                  | M      | jason-te-sde |
| [Issue #98: Migrate report endpoint to GET /api/pipelines/{name}/runs/...](https://github.com/CS7580-SEA-SP26/f-team/issues/98)                                    | M      | jason-te-sde |
| [Issue #99: Write Dockerfile to containerize the server](https://github.com/CS7580-SEA-SP26/f-team/issues/99)                                                      | S      | jason-te-sde |
| [Issue #100: Write Kubernetes manifests to deploy server and MongoDB to Minikube](https://github.com/CS7580-SEA-SP26/f-team/issues/100)                             | M      | jason-te-sde |
| [Issue #101: Package Kubernetes manifests as a Helm chart](https://github.com/CS7580-SEA-SP26/f-team/issues/101)                                                   | M      | jason-te-sde |
| [Issue #102: Support configurable server URL via CICD_SERVER_URL environment variable](https://github.com/CS7580-SEA-SP26/f-team/issues/102)                       | S      | jason-te-sde |
| [Issue #107: Add k8s Jobs executor for pipeline job execution (dual mode)](https://github.com/CS7580-SEA-SP26/f-team/issues/107)                                   | M      | jason-te-sde |

# Carry over tasks

| Task | Weight | Assignee |
| ---- | ------ | -------- |
|      |        |          |



# What worked this week?

### 1. Strangler Fig pattern for API migration
We migrated all four endpoints (verify, dryrun, run, report) from GET-with-file-paths to POST-with-body one at a time, keeping old GET routes working throughout. Each intermediate state was fully testable.

* **Why it worked:** This incremental approach eliminated the risk of a big-bang migration. Each issue could be independently tested and merged, and if any single endpoint broke, the others remained unaffected.

### 2. Sequential branch chaining (issue-95 → 96 → 97 → 98)
Instead of creating a parent branch and managing parallel feature branches, we chained issues sequentially so each branch inherited the previous one's changes.

* **Why it worked:** It avoided merge conflicts entirely and kept the workflow simple. The final branch contained all changes, resulting in a single clean PR to dev.

### 3. End-to-end testing at each step
Every issue was tested with both the CLI and direct curl commands before moving to the next one, including edge cases like paths with spaces (which caught the URL decoding bug in #97).

* **Why it worked:** Catching the `getQueryParam` URL decoding bug during #97 testing prevented it from becoming a harder-to-diagnose issue later in the k8s deployment phase.

# What did not work this week?

### 1. Dockerfile referenced non-existent directory
The initial Dockerfile included `COPY config/ config/` for a directory that does not exist in the repository, causing the first Docker build to fail.

* **Why it didn't work well:** The Dockerfile was written based on assumptions about the project structure (checkstyle config) rather than verifying what actually exists in the repo. This required an extra fix commit.

# Design updates

The following design document was created to describe architectural changes made this week:

- [Week 8 Design Update](../../design/week8-design-update.md) — Covers RESTful API migration (POST + path-based routing) and Kubernetes deployment architecture (Dockerfile, k8s manifests, Helm chart, CLI env var).