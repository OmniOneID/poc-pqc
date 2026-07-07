#!/bin/bash
set -euo pipefail

# Scripts to update configurations
# 1. Inject absolute wallet paths into application.yml
# 2. Inject User IP into configs (TAS URL and other services)
# 3. Inject springdoc.server.url for swagger-ui servers

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIGS_DIR="$PROJECT_ROOT/configs"
JARS_DIR="$PROJECT_ROOT/jars"
MARKER_FILE="$PROJECT_ROOT/.initialized"

USER_IP=""
TAS_PORT="${TAS_PORT:-4040}"

echo "============================================================"
echo ">> [Config] Updating Configurations..."
echo "============================================================"

# --- Part 1: Update Wallet Paths ---
echo ">> [1/3] Updating Wallet File Paths..."

# Helper to map config folder name to jars folder name
get_jar_folder() {
    case "$1" in
        "tas") echo "TA" ;;
        "cas") echo "CA" ;;
        "issuer") echo "Issuer" ;;
        "verifier") echo "Verifier" ;;
        "wallet") echo "Wallet" ;;
        "op") echo "OP" ;;
        *) echo "" ;;
    esac
}

for config_dir in "$CONFIGS_DIR"/*; do
    if [ -d "$config_dir" ]; then
        server_name=$(basename "$config_dir")
        yml_file="$config_dir/application.yml"

        if [ -f "$yml_file" ]; then
            if grep -q "wallet:" "$yml_file" && grep -q "file-path:" "$yml_file"; then
                jar_folder=$(get_jar_folder "$server_name")

                if [ -n "$jar_folder" ]; then
                    wallet_path="$JARS_DIR/$jar_folder/$server_name.wallet"
                    echo "   Updating $server_name -> $wallet_path"

                    escaped_path=$(echo "$wallet_path" | sed 's/\//\\\//g')

                    if [[ "$OSTYPE" == "darwin"* ]]; then
                        sed -i '' "s/^[[:space:]]*file-path:.*/  file-path: $escaped_path/" "$yml_file"
                    else
                        sed -i "s/^[[:space:]]*file-path:.*/  file-path: $escaped_path/" "$yml_file"
                    fi
                fi
            fi
        fi
    fi
done

# --- Part 2: Determine SERVER_BASE_URL ---
echo ""
echo ">> [2/4] Updating Server URL Configuration..."

if [ ! -f "$MARKER_FILE" ]; then
    # First run: URL is mandatory
    while true; do
        read -p "   Enter server base URL (e.g., http://10.48.17.123 or https://example.com): " INPUT_URL
        if [ -n "$INPUT_URL" ]; then
            break
        fi
        echo "   ERROR: Server base URL is required on first run."
    done
else
    # Re-run: read stored URL and allow keeping it
    CURRENT_URL=$(cat "$MARKER_FILE")
    read -p "   Press Enter to keep current URL ($CURRENT_URL), or enter a new one: " INPUT_URL
    if [ -z "$INPUT_URL" ]; then
        INPUT_URL="$CURRENT_URL"
    fi
fi

# Normalize URL
# 1. Add http:// if no protocol specified
if [[ ! "$INPUT_URL" =~ ^https?:// ]]; then
    INPUT_URL="http://$INPUT_URL"
fi

# 2. Remove trailing slash
SERVER_BASE_URL="${INPUT_URL%/}"

# 3. Extract protocol and host for compatibility
if [[ "$SERVER_BASE_URL" =~ ^(https?://)(.+)$ ]]; then
    URL_PROTOCOL="${BASH_REMATCH[1]}"
    URL_HOST="${BASH_REMATCH[2]}"
else
    echo "   ERROR: Invalid URL format: $SERVER_BASE_URL"
    exit 1
fi

# --- Part 3: Apply URL to all configs ---
echo ""
echo ">> [3/4] Applying URL to configs..."

# Update TAS URL in all configs
TAS_URL="${SERVER_BASE_URL}:${TAS_PORT}"
for config_dir in "$CONFIGS_DIR"/*; do
    if [ -d "$config_dir" ]; then
        yml_file="$config_dir/application.yml"
        if [ -f "$yml_file" ]; then
            if grep -q "tas:" "$yml_file" && grep -q "url:" "$yml_file"; then
                echo "   Updating TAS URL in $(basename "$config_dir")..."
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    sed -i '' -E "s|https?://[^:]+:${TAS_PORT}|${TAS_URL}|g" "$yml_file"
                else
                    sed -i -E "s|https?://[^:]+:${TAS_PORT}|${TAS_URL}|g" "$yml_file"
                fi
            fi
        fi
    fi
done

# Update URL in configs that use arbitrary ports (j-verifier, verification-server, proxy)
for server in "j-verifier" "verification-server" "proxy"; do
    yml_file="$CONFIGS_DIR/$server/application.yml"
    if [ -f "$yml_file" ]; then
        echo "   Updating $server URL -> $SERVER_BASE_URL"
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' -E "s|https?://[^:]+:|${SERVER_BASE_URL%://*}://${URL_HOST}:|g" "$yml_file"
        else
            sed -i -E "s|https?://[^:]+:|${SERVER_BASE_URL%://*}://${URL_HOST}:|g" "$yml_file"
        fi
    fi
done

# --- Part 4: Apply springdoc.server.url for swagger-ui servers ---
echo ""
echo ">> [4/4] Updating springdoc.server.url for swagger-ui servers..."

update_springdoc_url() {
    local server="$1"
    local port="$2"
    local yml_file="$CONFIGS_DIR/$server/application.yml"

    if [ ! -f "$yml_file" ]; then
        return
    fi

    local new_url="${SERVER_BASE_URL}:$port"

    if grep -q "^springdoc:" "$yml_file"; then
        # springdoc section exists — update URL in place
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' -E "s|https?://[^:]+:${port}|${new_url}|g" "$yml_file"
        else
            sed -i -E "s|https?://[^:]+:${port}|${new_url}|g" "$yml_file"
        fi
    else
        # No springdoc section yet — append it
        printf "\nspringdoc:\n  server:\n    url: %s\n" "$new_url" >> "$yml_file"
    fi

    echo "   Updated $server -> springdoc.server.url: $new_url"
}

update_springdoc_url "tas"      "4040"
update_springdoc_url "issuer"   "4242"
update_springdoc_url "verifier" "6060"
update_springdoc_url "api"      "6363"
update_springdoc_url "cas"      "5151"
update_springdoc_url "wallet"   "5252"
update_springdoc_url "op"       "7272"
update_springdoc_url "lss"      "4141"

# Save IP to marker file
echo "$SERVER_BASE_URL" > "$MARKER_FILE"
echo "   [OK] Server base URL set to: $SERVER_BASE_URL"

echo "============================================================"
echo "Config update complete."
