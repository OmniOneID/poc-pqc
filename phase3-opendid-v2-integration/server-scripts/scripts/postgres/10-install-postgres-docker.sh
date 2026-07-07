#!/bin/bash
set -euo pipefail

# -- env defaults --
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-opendid-poc-postgres}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16}"
POSTGRES_PORT="${POSTGRES_PORT:-5430}"
POSTGRES_USER="${POSTGRES_USER:-omn}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-omn}"

# Data volume name
VOLUME_NAME="opendid-poc-postgres-data"

echo "============================================================"
echo ">> [10] Installing PostgreSQL via Docker..."
echo "Container: ${POSTGRES_CONTAINER_NAME}"
echo "Image:     ${POSTGRES_IMAGE}"
echo "Port:      ${POSTGRES_PORT}"
echo "User:      ${POSTGRES_USER}"
echo "============================================================"

# 1. Check if container already exists
if docker ps -a --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER_NAME}$"; then
    echo "INFO: Container '${POSTGRES_CONTAINER_NAME}' already exists."

    # Check if runnning
    if [ "$(docker inspect -f '{{.State.Running}}' "${POSTGRES_CONTAINER_NAME}")" = "true" ]; then
        echo "INFO: Container is already running. PASS."
    else
        echo "INFO: Container is stopped. Starting it..."
        docker start "${POSTGRES_CONTAINER_NAME}"
        echo "INFO: Started existing container."
    fi
else
    echo "INFO: Container does not exist. Creating new container..."

    # Create volume if not exists
    if ! docker volume ls -q | grep -q "^${VOLUME_NAME}$"; then
        echo "INFO: Creating docker volume '${VOLUME_NAME}'..."
        docker volume create "${VOLUME_NAME}" || true
    fi

    # Run container
    # NOTE: Using --name, -p, -v, -e for configuration
    docker run -d \
      --name "${POSTGRES_CONTAINER_NAME}" \
      -p "${POSTGRES_PORT}:5432" \
      -v "${VOLUME_NAME}:/var/lib/postgresql/data" \
      -e POSTGRES_USER="${POSTGRES_USER}" \
      -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
      "${POSTGRES_IMAGE}"

    echo "SUCCESS: PostgreSQL container started."
fi

# Wait for readiness
echo "INFO: Waiting for PostgreSQL to be ready..."
TIMEOUT=60
ELAPSED=0

until [ $ELAPSED -ge $TIMEOUT ]; do
    if docker exec "${POSTGRES_CONTAINER_NAME}" pg_isready -U "${POSTGRES_USER}" > /dev/null 2>&1; then
        echo "SUCCESS: PostgreSQL is ready!"
        exit 0
    fi
    echo "Waiting... (${ELAPSED}s)"
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

echo "ERROR: Timeout waiting for PostgreSQL to be ready."
exit 1
