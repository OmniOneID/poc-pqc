#!/bin/bash
set -u

# Script to start OpenDID PoC Servers in dependency order.
# Usage: ./start-servers.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
JARS_DIR="$PROJECT_ROOT/jars"
CONFIGS_DIR="$PROJECT_ROOT/configs"
LOGS_DIR="$PROJECT_ROOT/logs"

# Load common functions
source "$SCRIPT_DIR/server-lib.sh"

echo "============================================================"
echo ">> Starting OpenDID PoC Servers..."
echo "============================================================"

# Check if we need to ensure PostgreSQL is running (only if installed locally)
DB_CONFIG_FILE="$PROJECT_ROOT/.db-config"
if [ -f "$DB_CONFIG_FILE" ]; then
    source "$DB_CONFIG_FILE"
    if [ "${INSTALL_DB:-yes}" = "yes" ]; then
        ensure_postgres_running
        echo ""
    else
        echo ">> [PostgreSQL] Skipping - using external database"
        echo ""
    fi
else
    # If no config file, assume local PostgreSQL
    ensure_postgres_running
    echo ""
fi

# --- Execution Order (dependency-aware) ---
# LSS -> TAS -> LSS(restart, to sync) -> ISSUER -> VERIFIER -> CAS -> WALLET -> API -> DEMO -> OBSERVER

# 1. LSS (first boot)
start_server "lss"

# 2. TAS
start_server "tas"

# 3. LSS (restart to sync DID with TAS)
echo ">> [Restarting LSS] to sync with TAS..."
start_server "lss" "true"

# 4. ISSUER
start_server "issuer"

# 5. VERIFIER
start_server "verifier"

# 6. CAS
start_server "cas"

# 7. WALLET
start_server "wallet"

# 8. API
start_server "api"

# 9. DEMO
start_server "demo"

# 10. OBSERVER
start_server "observer"

echo "============================================================"
echo ">> All servers started."
echo "   Please check logs in $LOGS_DIR for status."
echo "============================================================"
