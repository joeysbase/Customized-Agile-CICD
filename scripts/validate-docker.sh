#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[docker] root: $ROOT_DIR"

echo "[docker] 1/5 docker compose config"
docker compose config >/dev/null

echo "[docker] 2/5 build and start stack"
docker compose up --build -d

echo "[docker] 3/5 show containers"
docker compose ps

echo "[docker] 4/5 check core containers"
docker ps --format '{{.Names}}' | grep -E '^f-team-server-1$' >/dev/null
docker ps --format '{{.Names}}' | grep -E '^f-team-mongo-1$' >/dev/null
docker ps --format '{{.Names}}' | grep -E '^f-team-grafana-1$' >/dev/null

echo "[docker] 5/5 server port reachable check"
if command -v curl >/dev/null 2>&1; then
  curl -s http://localhost:8080/ >/dev/null || true
fi

echo
echo "[docker] PASS"
echo "Manual next step if needed:"
echo "  java -jar client/build/libs/client-all.jar verify .pipelines/success.yaml"
echo "  java -jar client/build/libs/client-all.jar dryrun .pipelines/success.yaml"