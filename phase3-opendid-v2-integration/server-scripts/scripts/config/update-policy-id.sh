#!/bin/bash
set -euo pipefail

# Script to update Verifier Policy ID

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIGS_DIR="$PROJECT_ROOT/configs"
DATAPORTAL_CONFIG="$CONFIGS_DIR/dataportal/application.yml"

echo "============================================================"
echo ">> [Config] Updating Policy ID..."
echo "============================================================"
echo ""
echo "WARNING: DataPortal server has been removed from this project."
echo "         This script is deprecated and may not work correctly."
echo ""

if [ ! -f "$DATAPORTAL_CONFIG" ]; then
    echo "ERROR: DataPortal config not found at $DATAPORTAL_CONFIG"
    echo "       (This is expected as DataPortal has been removed)"
    exit 1
fi

# Ask for Policy ID
read -p "Enter Verifier Policy ID (e.g., policy-1234): " POLICY_ID

if [ -z "$POLICY_ID" ]; then
    echo "ERROR: Policy ID cannot be empty."
    exit 1
fi

echo "   Injecting Policy ID: $POLICY_ID into $DATAPORTAL_CONFIG"

# Replace verifier_policy_id: <value>
# Regex: verifier_policy_id: .*
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s/^verifier_policy_id:.*/verifier_policy_id: $POLICY_ID/" "$DATAPORTAL_CONFIG"
else
    sed -i "s/^verifier_policy_id:.*/verifier_policy_id: $POLICY_ID/" "$DATAPORTAL_CONFIG"
fi

echo "   [OK] Policy ID Updated."
echo "============================================================"
