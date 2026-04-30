#!/bin/bash
set -euo pipefail

# OpenDID PoC Environment - PostgreSQL Installation
# This script installs PostgreSQL in a Docker container.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "============================================================"
echo ">> PostgreSQL Installation"
echo "============================================================"
echo ""
echo "This will install PostgreSQL in a Docker container."
echo ""

# Check if already installed
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
POSTGRES_CONFIG_FILE="$PROJECT_ROOT/.postgres-config"

if [ -f "$POSTGRES_CONFIG_FILE" ]; then
    source "$POSTGRES_CONFIG_FILE"
    if [ -n "${CONTAINER_NAME:-}" ]; then
        # Check if container exists
        if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
            echo "WARNING: PostgreSQL container '$CONTAINER_NAME' already exists."
            echo ""
            read -p "Do you want to reinstall? (yes/no) [no]: " REINSTALL
            REINSTALL="${REINSTALL:-no}"
            if [ "$REINSTALL" != "yes" ]; then
                echo ""
                echo "Installation cancelled. Existing container preserved."
                echo ""
                echo "To start the existing container, use:"
                echo "  docker start $CONTAINER_NAME"
                echo ""
                exit 0
            fi
            echo ""
            echo "Removing existing container..."
            docker stop "$CONTAINER_NAME" 2>/dev/null || true
            docker rm "$CONTAINER_NAME" 2>/dev/null || true
        fi
    fi
fi

# Run PostgreSQL installation
"$SCRIPT_DIR/postgres/run-all.sh"

echo ""
echo "============================================================"
echo ">> PostgreSQL Installation Complete!"
echo "============================================================"
echo ""
echo "PostgreSQL is now running in a Docker container."
echo ""
echo "Next steps:"
echo "  ./setup-local-db.sh    # Create databases and configure"
echo "============================================================"
