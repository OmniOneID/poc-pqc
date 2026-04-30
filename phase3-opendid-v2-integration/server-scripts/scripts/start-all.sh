#!/bin/bash
set -euo pipefail

# OpenDID PoC Environment - Start Script
# Usage: ./start-all.sh [wallet_password]

PASSWORD="${1:-omnioneopendid12!@}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DB_CONFIG_FILE="$PROJECT_ROOT/.db-config"

echo "============================================================"
echo ">> Starting OpenDID PoC Environment..."
echo "============================================================"

# Check if database is configured
if [ ! -f "$DB_CONFIG_FILE" ]; then
    echo ""
    echo "ERROR: Database is not configured."
    echo ""
    echo "Please run one of the following setup scripts first:"
    echo "  ./setup-local-db.sh      # Install PostgreSQL locally"
    echo "  ./setup-external-db.sh   # Configure external database"
    echo ""
    exit 1
fi

# Load database configuration
source "$DB_CONFIG_FILE"
echo ""
echo ">> Database Configuration:"
if [ "${INSTALL_DB:-}" = "yes" ]; then
    echo "   Type: Local PostgreSQL"
else
    echo "   Type: External Database"
fi
echo "   Host: ${DB_HOST:-unknown}"
echo "   Port: ${DB_PORT:-unknown}"
echo ""

# 1. Wallet/Tool Setup
echo ">> [Step 1] Generating Wallets and Keys..."
# The script create-all-wallets.sh handles idempotency (skips if exists)
"$SCRIPT_DIR/tool/create-all-wallets.sh" "$PASSWORD"

echo ""
# 2. Update Configurations
echo ">> [Step 2] Updating Configurations..."
# Prompts for IP and updates application.yml files
"$SCRIPT_DIR/config/update-configs.sh"

echo ""
# 3. Server Start
echo ">> [Step 3] Starting Servers..."
"$SCRIPT_DIR/server/start-servers.sh"

echo ""
echo "============================================================"
echo ">> ALL SERVERS STARTED."
echo "============================================================"
