#!/bin/bash
set -u

# Shortcut to stop servers.
# Usage:
#   ./stop-all.sh           → Stop ALL servers
#   ./stop-all.sh <name>    → Stop a specific server (delegates to server.sh)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "${1:-}" != "" ]; then
    # Individual server: delegate to server.sh
    exec "$SCRIPT_DIR/server.sh" stop "$1"
else
    # All servers
    echo ">> Calling server stop script..."
    "$SCRIPT_DIR/server/stop-servers.sh"
fi
