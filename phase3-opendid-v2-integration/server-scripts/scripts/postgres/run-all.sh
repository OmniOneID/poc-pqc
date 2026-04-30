#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
POSTGRES_CONFIG_FILE="$PROJECT_ROOT/.postgres-config"

echo "############################################################"
echo "# OpenDID PoC Environment Setup - PostgreSQL"
echo "############################################################"

# --- Determine PostgreSQL container name and image ---
if [ ! -f "$POSTGRES_CONFIG_FILE" ]; then
    # First run: input required
    while true; do
        read -p "   Enter PostgreSQL container name (e.g., opendid-poc-postgres): " INPUT_CONTAINER
        if [ -n "$INPUT_CONTAINER" ]; then
            break
        fi
        echo "   ERROR: Container name is required on first run."
    done

    while true; do
        read -p "   Enter PostgreSQL image (e.g., postgres:16): " INPUT_IMAGE
        if [ -n "$INPUT_IMAGE" ]; then
            break
        fi
        echo "   ERROR: PostgreSQL image is required on first run."
    done
else
    # Re-run: read stored values and allow keeping them
    CURRENT_CONTAINER=$(grep "^CONTAINER_NAME=" "$POSTGRES_CONFIG_FILE" | cut -d'=' -f2)
    CURRENT_IMAGE=$(grep "^IMAGE=" "$POSTGRES_CONFIG_FILE" | cut -d'=' -f2)

    read -p "   Press Enter to keep container name ($CURRENT_CONTAINER), or enter a new one: " INPUT_CONTAINER
    if [ -z "$INPUT_CONTAINER" ]; then
        INPUT_CONTAINER="$CURRENT_CONTAINER"
    fi

    read -p "   Press Enter to keep PostgreSQL image ($CURRENT_IMAGE), or enter a new one: " INPUT_IMAGE
    if [ -z "$INPUT_IMAGE" ]; then
        INPUT_IMAGE="$CURRENT_IMAGE"
    fi
fi

# Save to config file
cat > "$POSTGRES_CONFIG_FILE" <<EOF
CONTAINER_NAME=${INPUT_CONTAINER}
IMAGE=${INPUT_IMAGE}
EOF

echo "   [OK] Container: $INPUT_CONTAINER / Image: $INPUT_IMAGE"
echo ""

# Export for child scripts
export POSTGRES_CONTAINER_NAME="$INPUT_CONTAINER"
export POSTGRES_IMAGE="$INPUT_IMAGE"

# Execute scripts in order
"$SCRIPT_DIR/00-check-prereq.sh"
echo ""
"$SCRIPT_DIR/10-install-postgres-docker.sh"
echo ""
"$SCRIPT_DIR/20-init-databases.sh"
echo ""
"$SCRIPT_DIR/99-postgres-status.sh"

echo ""
echo "############################################################"
echo "# ALL DONE!"
echo "############################################################"
