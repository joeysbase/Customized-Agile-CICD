# Final Submission Validation Checklist

Use these checks before final submission to reduce regressions in the highest-risk flows.

## 1. Local developer build/check path
Run:

```bash
./scripts/validate-local.sh
```
## 2. CLI example validation path

Run:

```bash
./scripts/validate-cli.sh
```
## 3. Docker image / compose path

Run:

```bash
./scripts/validate-docker.sh
```
## 4. Raw Kubernetes deployment validation path

Run:

```bash
./scripts/validate-k8s.sh
```
## 5. Helm deployment validation path

Run:

```bash
./scripts/validate-helm.sh
```