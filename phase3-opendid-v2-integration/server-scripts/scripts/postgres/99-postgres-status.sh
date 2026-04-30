#!/bin/bash
set -euo pipefail

# -- env defaults --
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-opendid-poc-postgres}"
POSTGRES_USER="${POSTGRES_USER:-omn}"
export PGPASSWORD="${POSTGRES_PASSWORD:-omn}"

DB_LIST=("tas" "issuer" "verifier" "cas" "wallet" "lss" "op")

echo "============================================================"
echo ">> [99] Checking PostgreSQL Status..."
echo "============================================================"

# 1. Container Status
if docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER_NAME}$"; then
    STATUS=$(docker inspect -f '{{.State.Status}}' "${POSTGRES_CONTAINER_NAME}")
    echo "Container '${POSTGRES_CONTAINER_NAME}': exists (Status: ${STATUS})"
    if [ "$STATUS" != "running" ]; then
        echo "WARNING: Container is not running."
    fi
else
    echo "ERROR: Container '${POSTGRES_CONTAINER_NAME}' does not exist."
    exit 1
fi

# 2. Database Status
echo "------------------------------------------------------------"
echo "Checking Databases..."
MISSING_COUNT=0

for db in "${DB_LIST[@]}"; do
    if docker exec -e PGPASSWORD="${PGPASSWORD}" -i "${POSTGRES_CONTAINER_NAME}" \
        psql -U "${POSTGRES_USER}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${db}'" | grep -q "1"; then
        echo "[OK] $db"
    else
        echo "[MISSING] $db"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    fi
done

echo "------------------------------------------------------------"
if [ "$MISSING_COUNT" -eq 0 ]; then
    echo "SUCCESS: All 7 databases exist."
else
    echo "WARNING: $MISSING_COUNT databases are missing."
    exit 1
fi
