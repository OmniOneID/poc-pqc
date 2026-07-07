#!/bin/bash
set -u

# Script to stop all OpenDID PoC Servers.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
JARS_DIR="$PROJECT_ROOT/jars"
CONFIGS_DIR="$PROJECT_ROOT/configs"
LOGS_DIR="$PROJECT_ROOT/logs"

# Load common functions
source "$SCRIPT_DIR/server-lib.sh"

echo "============================================================"
echo ">> Stopping OpenDID PoC Servers..."
echo "============================================================"

for server in "${VALID_SERVERS[@]}"; do
    stop_server "$server"
done

echo "============================================================"
echo ">> All servers stopped."
echo "============================================================"
