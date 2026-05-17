#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[local] root: $ROOT_DIR"

echo "[local] 1/5 ensure gradlew executable"
chmod +x ./gradlew

echo "[local] 2/5 build all"
./gradlew clean build --no-daemon

echo "[local] 3/5 generate javadocs"
./gradlew javadoc --no-daemon

echo "[local] 4/5 build client jar"
./gradlew :client:jar --no-daemon

echo "[local] 5/5 build server jar"
./gradlew :server:jar --no-daemon

echo
echo "[local] PASS"
echo "Artifacts:"
echo "  client/build/libs/client-all.jar"
echo "  server/build/libs/server-all.jar"