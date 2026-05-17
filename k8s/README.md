# Raw Kubernetes Deployment

This directory provides a raw Kubernetes deployment path for the full system
without Helm.

## What Gets Deployed

- MongoDB
- CI/CD server
- Prometheus
- Loki
- Tempo
- OpenTelemetry Collector
- Grafana

## Prerequisites

- A Kubernetes cluster with dynamic volume provisioning or a storage class that
  can satisfy the included PVCs
- `kubectl` configured for the target cluster
- Network access from the cluster to pull:
  - `mongo:7`
  - `jasonte/cs7580fteam:latest`
  - `prom/prometheus`
  - `grafana/loki`
  - `grafana/tempo`
  - `otel/opentelemetry-collector-contrib`
  - `grafana/grafana`

## Deploy With Raw Manifests

Apply all manifests in the `k8s/` directory:

```bash
kubectl apply -f k8s/
```

## Verify Core Resources

Wait for the deployments to become available:

```bash
kubectl get deployments
kubectl rollout status deployment/mongo
kubectl rollout status deployment/cicd-server
kubectl rollout status deployment/prometheus
kubectl rollout status deployment/loki
kubectl rollout status deployment/tempo
kubectl rollout status deployment/otel-collector
kubectl rollout status deployment/grafana
```

Verify the services:

```bash
kubectl get services
```

## Default Service Endpoints

- CI/CD server: `cicd-server-service:8080`
- MongoDB: `mongo-service:27017`
- Prometheus: `prometheus-service:9090`
- Loki: `loki-service:3100`
- Tempo: `tempo-service:3200`
- OTel Collector gRPC: `otel-collector-service:4317`
- OTel Collector HTTP: `otel-collector-service:4318`
- Grafana: `grafana-service:3000`

The server manifest is already configured to export traces to the in-cluster OTel
Collector at `http://otel-collector-service:4317`.

## Accessing the UI

The default manifests expose these services as `NodePort`:

- CI/CD server on port `30080`
- Grafana on port `30300`

You can also port-forward if NodePort access is not available:

```bash
kubectl port-forward service/cicd-server-service 8080:8080
kubectl port-forward service/grafana-service 3000:3000
```

Grafana defaults:

- Username: `admin`
- Password: `admin`

## Cleanup

Remove the raw deployment with:

```bash
kubectl delete -f k8s/
```
