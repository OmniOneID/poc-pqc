#!/bin/bash
set -euo pipefail

# Usage: ./create-all-wallets.sh <password>

PASSWORD="${1:-}"

if [ -z "$PASSWORD" ]; then
    echo "Usage: $0 <password>"
    echo "Please provide a password for wallet creation."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
JARS_DIR="$PROJECT_ROOT/jars"

# Dynamically locate CLI JAR (version-agnostic)
CLI_JAR=$(find "$JARS_DIR" -maxdepth 1 -name "did-cli-tool-server-*.jar" 2>/dev/null | head -n 1)

if [ -z "$CLI_JAR" ]; then
    echo "ERROR: CLI JAR not found in $JARS_DIR"
    echo "Please copy 'did-cli-tool-server-<version>.jar' to '$JARS_DIR'."
    exit 1
fi

echo "============================================================"
echo ">> Creating Wallets, Keys, and DIDs..."
echo "JAR: $CLI_JAR"
echo "DIR: $JARS_DIR"
echo "============================================================"

# Define paths
TAS_PATH="$JARS_DIR/TA"
CAS_PATH="$JARS_DIR/CA"
ISSUER_PATH="$JARS_DIR/Issuer"
VERIFIER_PATH="$JARS_DIR/Verifier"
WALLET_PATH="$JARS_DIR/Wallet"
OP_PATH="$JARS_DIR/OP"

# 1. Create Directories
for dir in "$TAS_PATH" "$CAS_PATH" "$ISSUER_PATH" "$VERIFIER_PATH" "$WALLET_PATH" "$OP_PATH"; do
    if [ ! -d "$dir" ]; then
        echo "Creating directory: $dir"
        mkdir -p "$dir"
    fi
done

# Helper to run java command
run_cli() {
    echo "$PASSWORD" | java -jar "$CLI_JAR" "$@" -p > /dev/null
}

# Unified function to Create Wallet AND Add Keys
setup_wallet_and_keys() {
    local path="$1"
    local name="$2"
    shift 2
    local keys=("$@")
    
    local wallet_file="${path}/${name}.wallet"

    if [ -f "$wallet_file" ]; then
        echo "INFO: Wallet $wallet_file already exists. Skipping creation and key addition."
        return
    fi

    echo ">> Creating wallet: ${name}..."
    if run_cli walletManager createWallet -m "${path}/${name}"; then
        echo "   [OK] Wallet created."
    else
        echo "   [FAIL] Failed to create wallet."
        return 1
    fi

    # Add Keys
    for key in "${keys[@]}"; do
        echo "   -> Adding key: $key"
        run_cli walletManager addKey -m "$wallet_file" -i "$key" -t 1 || echo "      [WARNING] Failed to add key $key"
    done
}

# 2. Setup Wallets and Keys
echo ">> [1/2] Setting up Wallets and Keys..."

setup_wallet_and_keys "$TAS_PATH" "tas" "assert" "auth" "keyagree" "invoke"
setup_wallet_and_keys "$CAS_PATH" "cas" "assert" "auth" "keyagree"
setup_wallet_and_keys "$ISSUER_PATH" "issuer" "assert" "auth" "keyagree"
setup_wallet_and_keys "$VERIFIER_PATH" "verifier" "assert" "auth" "keyagree"
setup_wallet_and_keys "$WALLET_PATH" "wallet" "assert" "auth" "keyagree"
setup_wallet_and_keys "$OP_PATH" "op" "assert" "auth" "keyagree"


# 3. Create DIDs
echo ">> [2/2] Creating DIDs..."

create_did_safe() {
    local wallet_path="$1"
    local did_file="$2"
    local did_id="$3"
    local controller="$4"
    shift 4
    local args=("$@")

    if [ -f "$did_file" ]; then
        echo "INFO: DID file $did_file already exists. Skipping."
    else
        echo "Creating DID: $did_id"
        run_cli did createDid -m "$wallet_path" -f "$did_file" -id "$did_id" -ci "$controller" "${args[@]}" || echo "WARNING: Failed to create DID $did_id"
    fi
}

create_did_safe "$TAS_PATH/tas.wallet" "$TAS_PATH/tas.did" "did:omn:tas" "did:omn:tas" -mi assert -ai auth -ki keyagree -ii invoke
create_did_safe "$CAS_PATH/cas.wallet" "$CAS_PATH/cas.did" "did:omn:cas" "did:omn:tas" -mi assert -ai auth -ki keyagree
create_did_safe "$ISSUER_PATH/issuer.wallet" "$ISSUER_PATH/issuer.did" "did:omn:issuer" "did:omn:tas" -mi assert -ai auth -ki keyagree
create_did_safe "$VERIFIER_PATH/verifier.wallet" "$VERIFIER_PATH/verifier.did" "did:omn:verifier" "did:omn:tas" -mi assert -ai auth -ki keyagree
create_did_safe "$WALLET_PATH/wallet.wallet" "$WALLET_PATH/wallet.did" "did:omn:wallet" "did:omn:tas" -mi assert -ai auth -ki keyagree
create_did_safe "$OP_PATH/op.wallet" "$OP_PATH/op.did" "did:omn:op" "did:omn:tas" -mi assert -ai auth -ki keyagree

echo "============================================================"
echo "SUCCESS: Wallet and DID generation complete."
echo "============================================================"
