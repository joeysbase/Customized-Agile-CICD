#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="cicd-mongo"
IMAGE_NAME="mongo:7"
HOST_PORT="27017"
CONTAINER_PORT="27017"

echo "Checking Docker..."

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: Docker is not installed or not in PATH." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Error: Docker daemon is not running." >&2
  exit 1
fi

# If container is already running, do nothing
if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "MongoDB container '$CONTAINER_NAME' is already running."
  exit 0
fi

# If container exists but is stopped, start it
if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "Starting existing MongoDB container '$CONTAINER_NAME'..."
  docker start "$CONTAINER_NAME" >/dev/null
else
  echo "Creating and starting MongoDB container '$CONTAINER_NAME'..."
  docker run -d \
    --name "$CONTAINER_NAME" \
    -p "${HOST_PORT}:${CONTAINER_PORT}" \
    "$IMAGE_NAME" >/dev/null
fi

echo "Waiting for MongoDB to become ready..."
sleep 5

echo "MongoDB should now be available at localhost:${HOST_PORT}"
docker ps --filter "name=$CONTAINER_NAME"