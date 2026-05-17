#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

CLIENT_JAR="$ROOT_DIR/client/build/libs/client-all.jar"

echo "[cli] root: $ROOT_DIR"

if [[ ! -f "$CLIENT_JAR" ]]; then
  echo "[cli] client jar missing, building it now..."
  chmod +x ./gradlew
  ./gradlew :client:jar --no-daemon
fi

echo "[cli] 1/4 help"
java -jar "$CLIENT_JAR" --help >/dev/null

echo "[cli] 2/4 verify success pipeline"
java -jar "$CLIENT_JAR" verify .pipelines/success.yaml

echo "[cli] 3/4 dryrun success pipeline"
java -jar "$CLIENT_JAR" dryrun .pipelines/success.yaml

echo "[cli] 4/4 report help shape"
java -jar "$CLIENT_JAR" report --help >/dev/null

echo
echo "[cli] PASS"