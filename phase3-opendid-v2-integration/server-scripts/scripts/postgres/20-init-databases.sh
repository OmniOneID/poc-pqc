#!/bin/bash
set -euo pipefail

# =============================================================
# 20-init-databases.sh
# Usage:
#   ./20-init-databases.sh               → 전체 DB 생성
#   ./20-init-databases.sh db1 db2 ...   → 지정한 DB만 생성
# =============================================================

# -- env defaults --
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-opendid-poc-postgres}"
POSTGRES_USER="${POSTGRES_USER:-omn}"
# Note: PGPASSWORD is used by psql to authenticate
export PGPASSWORD="${POSTGRES_PASSWORD:-omn}"

# 인자가 있으면 해당 DB만, 없으면 전체
if [ $# -gt 0 ]; then
    DB_LIST=("$@")
else
    DB_LIST=("tas" "issuer" "verifier" "cas" "wallet" "lss" "op")
fi

echo "============================================================"
echo ">> [20] Initializing Databases..."
echo "Container: ${POSTGRES_CONTAINER_NAME}"
echo "User:      ${POSTGRES_USER}"
echo "Databases: ${DB_LIST[*]}"
echo "============================================================"

# Helper function to execute SQL
exec_sql() {
    local sql="$1"
    local dbname="${2:-postgres}"
    docker exec -e PGPASSWORD="${PGPASSWORD}" -i "${POSTGRES_CONTAINER_NAME}" \
        psql -U "${POSTGRES_USER}" -d "${dbname}" -c "${sql}"
}

# Helper function to check if DB exists
check_db_exists() {
    local dbname="$1"
    local result
    result=$(docker exec -e PGPASSWORD="${PGPASSWORD}" -i "${POSTGRES_CONTAINER_NAME}" \
        psql -U "${POSTGRES_USER}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${dbname}'")
    [ "$result" == "1" ]
}

# 1. Wait for readiness (just in case)
if ! docker exec "${POSTGRES_CONTAINER_NAME}" pg_isready -U "${POSTGRES_USER}" > /dev/null 2>&1; then
    echo "ERROR: PostgreSQL container is not ready. Please run 10-install-postgres-docker.sh first."
    exit 1
fi

# 2. Iterate and Create Databases
for db in "${DB_LIST[@]}"; do
    if check_db_exists "$db"; then
        echo "INFO: Database '$db' already exists. PASS."
    else
        echo "INFO: Creating database '$db'..."
        # Create database with owner
        exec_sql "CREATE DATABASE \"$db\" OWNER \"${POSTGRES_USER}\";" "postgres"
        echo "SUCCESS: Database '$db' created."
    fi
done

echo "SUCCESS: All databases processed."
