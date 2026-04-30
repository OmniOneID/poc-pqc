#!/bin/bash
set -euo pipefail

echo "============================================================"
echo ">> [00] Checking Prerequisites..."
echo "============================================================"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed or not in PATH."
    exit 1
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    echo "ERROR: Docker daemon is not running."
    exit 1
fi

echo "SUCCESS: Docker is installed and running."
