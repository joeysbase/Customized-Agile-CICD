#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[k8s] root: $ROOT_DIR"

echo "[k8s] 1/6 cluster info"
kubectl cluster-info >/dev/null

echo "[k8s] 2/6 apply manifests"
kubectl apply -f k8s/

echo "[k8s] 3/6 wait for core deployments"
kubectl rollout status deployment/mongo --timeout=180s
kubectl rollout status deployment/cicd-server --timeout=180s
kubectl rollout status deployment/grafana --timeout=180s || true

echo "[k8s] 4/6 list deployments"
kubectl get deployments

echo "[k8s] 5/6 list services"
kubectl get services

echo "[k8s] 6/6 verify expected services exist"
kubectl get svc cicd-server-service >/dev/null
kubectl get svc grafana-service >/dev/null

echo
echo "[k8s] PASS"
echo "Port-forward examples:"
echo "  kubectl port-forward svc/cicd-server-service 8080:8080"
echo "  kubectl port-forward svc/grafana-service 3000:3000"