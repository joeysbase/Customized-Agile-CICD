#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[helm] root: $ROOT_DIR"

echo "[helm] 1/7 cluster info"
kubectl cluster-info >/dev/null

echo "[helm] 2/7 render chart"
helm template cicd helm/cicd-chart >/dev/null

echo "[helm] 3/7 remove raw k8s resources if present"
kubectl delete -f k8s/ >/dev/null 2>&1 || true

echo "[helm] 4/7 remove old helm release if present"
helm uninstall cicd >/dev/null 2>&1 || true

echo "[helm] 5/7 install chart"
helm install cicd helm/cicd-chart/

echo "[helm] 6/7 show deployments"
kubectl get deployments

echo "[helm] 7/7 show services"
kubectl get services

echo "[helm] verify expected helm services exist"
kubectl get svc cicd-cicd-server-service >/dev/null
kubectl get svc cicd-grafana-service >/dev/null

echo
echo "[helm] PASS"
echo "Port-forward examples:"
echo "  kubectl port-forward svc/cicd-cicd-server-service 8080:8080"
echo "  kubectl port-forward svc/cicd-grafana-service 3000:3000"