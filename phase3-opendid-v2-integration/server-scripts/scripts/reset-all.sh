#!/bin/bash
set -u

# =============================================================
# reset-all.sh - OpenDID PoC Full Reset
# =============================================================
# Stops all servers and drops/recreates all databases.
# After this, restarting servers will trigger Liquibase to
# re-apply schemas from scratch.
#
# Usage:
#   ./reset-all.sh [-y] [--ip <ip>]
#
# Options:
#   -y, --yes    Skip the confirmation prompt
#   --ip <ip>    Server IP address (passed to setup-all.sh)
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
POSTGRES_CONFIG_FILE="$PROJECT_ROOT/.postgres-config"

SKIP_CONFIRM=false
_LOCAL_IP=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        -y|--yes)  SKIP_CONFIRM=true; shift ;;
        --ip)      _LOCAL_IP="$2"; shift 2 ;;
        -h|--help)
            sed -n '/^# Usage:/,/^# ====*/p' "$0" | sed 's/^# \?//'
            exit 0
            ;;
        *) echo "ERROR: Unknown option: $1"; exit 1 ;;
    esac
done

# --- env defaults ---
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-opendid-poc-postgres}"
POSTGRES_USER="${POSTGRES_USER:-omn}"
export PGPASSWORD="${POSTGRES_PASSWORD:-omn}"

if [ -f "$POSTGRES_CONFIG_FILE" ]; then
    _stored=$(grep "^CONTAINER_NAME=" "$POSTGRES_CONFIG_FILE" | cut -d'=' -f2)
    [ -n "$_stored" ] && POSTGRES_CONTAINER_NAME="$_stored"
fi

DB_LIST=("tas" "issuer" "verifier" "cas" "wallet" "lss")

echo "============================================================"
echo "  OpenDID PoC Full Reset"
echo "============================================================"
echo "  Container : $POSTGRES_CONTAINER_NAME"
echo "  User      : $POSTGRES_USER"
echo "  Databases : ${DB_LIST[*]}"
echo "============================================================"
echo ""
echo "  WARNING: This will STOP all servers and DROP all databases."
echo "           All data will be permanently lost."
echo ""

if [ "$SKIP_CONFIRM" != "true" ]; then
    read -rp "  Continue? [y/N] " CONFIRM
    if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
        echo "  Aborted."
        exit 0
    fi
fi

echo ""
echo "============================================================"
echo "  [1/5] Stopping all servers"
echo "============================================================"
"$SCRIPT_DIR/server/stop-servers.sh"

echo ""
echo "============================================================"
echo "  [2/5] Dropping databases"
echo "============================================================"

if ! command -v docker &>/dev/null; then
    echo "ERROR: docker command not found."
    exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER_NAME}$"; then
    echo "ERROR: PostgreSQL container '$POSTGRES_CONTAINER_NAME' is not running."
    echo "  Start it first with: scripts/postgres/run-all.sh"
    exit 1
fi

# Give lingering server processes a moment to release connections
sleep 2

exec_sql() {
    local sql="$1"
    local dbname="${2:-postgres}"
    docker exec -e PGPASSWORD="${PGPASSWORD}" -i "${POSTGRES_CONTAINER_NAME}" \
        psql -U "${POSTGRES_USER}" -d "${dbname}" -c "${sql}"
}

for db in "${DB_LIST[@]}"; do
    echo ">> Dropping '$db'..."
    exec_sql \
        "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${db}' AND pid <> pg_backend_pid();" \
        "postgres" > /dev/null 2>&1 || true
    exec_sql "DROP DATABASE IF EXISTS \"$db\";" "postgres"
done

echo ""
echo "============================================================"
echo "  [3/5] Recreating databases"
echo "============================================================"
POSTGRES_CONTAINER_NAME="$POSTGRES_CONTAINER_NAME" \
POSTGRES_USER="$POSTGRES_USER" \
POSTGRES_PASSWORD="$PGPASSWORD" \
    "$SCRIPT_DIR/postgres/20-init-databases.sh"

echo ""
echo "============================================================"
echo "  [4/5] Starting all servers"
echo "============================================================"
"$SCRIPT_DIR/server/start-servers.sh"

echo ""
echo "============================================================"
echo "  [5/5] Running setup"
echo "============================================================"
if [ -n "$_LOCAL_IP" ]; then
    "$SCRIPT_DIR/setup/setup-all.sh" --ip "$_LOCAL_IP"
else
    "$SCRIPT_DIR/setup/setup-all.sh"
fi

echo ""
echo "============================================================"
echo "  Reset complete."
echo "============================================================"
