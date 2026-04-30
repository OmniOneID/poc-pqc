#!/bin/bash
set -euo pipefail

# =============================================================
# setup-ta.sh - OpenDID PoC TA Server Setup Automation
# =============================================================
# Prerequisites:
#   - TA server must be running
#   - Setup Reset must have been performed beforehand
#
# Usage:
#   ./setup-ta.sh [options]
#
# Options:
#   --tas-url <url>           TA server base URL (default: http://localhost:8090/tas)
#   --admin-id <id>           Admin login ID (default: admin)
#   --initial-password <pw>   Initial password after Setup Reset
#   --new-password <pw>       Password to set
#   --ta-secret <hash>        SHA-256 hex hash of ta.auth.registration-password
#   --ta-name <name>          TA name (default: "OpenDID TA")
#   --ta-server-url <url>     TA server URL for registration (default: same as --tas-url)
#   --cert-dn <dn>            Certificate DN
#   --work-dir <dir>          Artifact output directory (default: <project_root>/logs/ta-setup)
#   -h, --help                Show this help
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# --- Defaults ---
_LOCAL_IP=""
TAS_URL=""
TA_ADMIN_ID="admin@opendid.omnione.net"
TA_ADMIN_INITIAL_PASSWORD=""
TA_ADMIN_NEW_PASSWORD=""
# SHA-256 hex hash of "VoOyEuOyal" (ta.auth.registration-password)
TA_SECRET="b6a37fd6291434c3c39246edb0014c4d53f613e4c7b6e6b845cb484619eb5ec0"
TA_NAME="OpenDID TA"
TA_SERVER_URL=""
TA_CERT_DN="CN=OpenDID TA,OU=OpenDID,O=OmniOne"
WORK_DIR="$PROJECT_ROOT/logs/ta-setup"

# --- Parse Arguments ---
while [[ $# -gt 0 ]]; do
    case "$1" in
        --ip)                 _LOCAL_IP="$2"; shift 2 ;;
        --tas-url)            TAS_URL="$2"; shift 2 ;;
        --admin-id)           TA_ADMIN_ID="$2"; shift 2 ;;
        --initial-password)   TA_ADMIN_INITIAL_PASSWORD="$2"; shift 2 ;;
        --new-password)       TA_ADMIN_NEW_PASSWORD="$2"; shift 2 ;;
        --ta-name)            TA_NAME="$2"; shift 2 ;;
        --ta-secret)
            echo "WARNING: --ta-secret is ignored. TA secret is fixed internally."
            shift 2
            ;;
        --ta-server-url)      TA_SERVER_URL="$2"; shift 2 ;;
        --cert-dn)            TA_CERT_DN="$2"; shift 2 ;;
        --work-dir)           WORK_DIR="$2"; shift 2 ;;
        -h|--help)
            sed -n '/^# Usage:/,/^# ====*/p' "$0" | sed 's/^# \?//'
            exit 0
            ;;
        *)
            echo "ERROR: Unknown option: $1"
            exit 1
            ;;
    esac
done

# TA_SERVER_URL defaults to TAS_URL (resolved after prompt, set later)


ADMIN_BASE=""
COOKIE_FILE="$WORK_DIR/cookies.txt"

# =============================================================
# Utility Functions
# =============================================================

fail() {
    echo ""
    echo "================================================================"
    echo "  FAILED: $1"
    echo "  Step   : ${CURRENT_STEP:-unknown}"
    echo "  URL    : ${LAST_URL:-unknown}"
    if [ -n "${LAST_BODY:-}" ]; then
        echo "  Request: $LAST_BODY"
    fi
    if [ -n "${LAST_STATUS:-}" ]; then
        echo "  HTTP   : $LAST_STATUS"
    fi
    if [ -n "${LAST_RESPONSE:-}" ]; then
        echo "  Response: $LAST_RESPONSE"
    fi
    echo "================================================================"
    exit 1
}

require_arg() {
    local name="$1"
    local value="$2"
    if [ -z "$value" ]; then
        echo "ERROR: Required argument missing: $name"
        echo "  Run with --help for usage."
        exit 1
    fi
}

save_artifact() {
    local filename="$1"
    local content="$2"
    echo "$content" > "$WORK_DIR/$filename"
}

assert_field() {
    local json="$1"
    local field="$2"
    local expected="$3"
    local actual
    actual=$(echo "$json" | jq -r "$field" 2>/dev/null)
    if [ "$actual" != "$expected" ]; then
        LAST_RESPONSE="$json"
        fail "Expected $field == '$expected', got '$actual'"
    fi
}

# post_json <step_label> <path> <body>
# Sets RESPONSE on success, calls fail() on HTTP error.
post_json() {
    CURRENT_STEP="$1"
    local path="$2"
    local body="$3"
    LAST_URL="$ADMIN_BASE$path"
    LAST_BODY="$body"

    echo ">> [$CURRENT_STEP]"

    local tmp_file
    tmp_file=$(mktemp)
    local http_code
    http_code=$(curl -s -o "$tmp_file" -w "%{http_code}" \
        -X POST "$LAST_URL" \
        -H "Content-Type: application/json" \
        -b "$COOKIE_FILE" -c "$COOKIE_FILE" \
        -d "$body")
    LAST_RESPONSE=$(cat "$tmp_file")
    rm -f "$tmp_file"
    LAST_STATUS="$http_code"

    if [[ "$http_code" != 2* ]]; then
        fail "HTTP $http_code"
    fi

    RESPONSE="$LAST_RESPONSE"
    echo "   [OK]"
}

# get_json <step_label> <path>
# Sets RESPONSE on success, calls fail() on HTTP error.
get_json() {
    CURRENT_STEP="$1"
    local path="$2"
    LAST_URL="$ADMIN_BASE$path"
    LAST_BODY=""

    echo ">> [$CURRENT_STEP]"

    local tmp_file
    tmp_file=$(mktemp)
    local http_code
    http_code=$(curl -s -o "$tmp_file" -w "%{http_code}" \
        -X GET "$LAST_URL" \
        -b "$COOKIE_FILE" -c "$COOKIE_FILE")
    LAST_RESPONSE=$(cat "$tmp_file")
    rm -f "$tmp_file"
    LAST_STATUS="$http_code"

    if [[ "$http_code" != 2* ]]; then
        fail "HTTP $http_code"
    fi

    RESPONSE="$LAST_RESPONSE"
    echo "   [OK]"
}

wait_for_health() {
    echo ">> [Health Check] Waiting for TA server at $ADMIN_BASE/health ..."
    local timeout=60
    local elapsed=0
    until curl -sf "$ADMIN_BASE/health" > /dev/null 2>&1; do
        if [ $elapsed -ge $timeout ]; then
            CURRENT_STEP="Health Check"
            LAST_URL="$ADMIN_BASE/health"
            fail "TA server did not respond within ${timeout}s"
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done
    echo "   [OK] TA server is up."
}

# =============================================================
# Validate Required Arguments
# =============================================================

# Resolve TAS_URL and TA_SERVER_URL
if [ -z "$_LOCAL_IP" ] && [ -z "$TAS_URL" ]; then
    read -rp "Server IP: " _LOCAL_IP
fi

if [ -z "$TAS_URL" ]; then
    TAS_URL="http://${_LOCAL_IP}:8090/tas"
fi

TA_SERVER_URL="${TA_SERVER_URL:-$TAS_URL}"
ADMIN_BASE="$TAS_URL/admin/v1"

if [ -z "$TA_ADMIN_INITIAL_PASSWORD" ]; then
    read -rsp "Initial password: " TA_ADMIN_INITIAL_PASSWORD
    echo ""
fi

if [ -z "$TA_ADMIN_NEW_PASSWORD" ]; then
    read -rsp "New password: " TA_ADMIN_NEW_PASSWORD
    echo ""
fi

# Passwords are stored as SHA-256 hashes in DB; hash before sending
TA_ADMIN_INITIAL_PASSWORD_HASH=$(echo -n "$TA_ADMIN_INITIAL_PASSWORD" | shasum -a 256 | awk '{print $1}')
TA_ADMIN_NEW_PASSWORD_HASH=$(echo -n "$TA_ADMIN_NEW_PASSWORD" | shasum -a 256 | awk '{print $1}')

if ! command -v jq &>/dev/null; then
    echo "ERROR: 'jq' is required but not installed."
    exit 1
fi

# =============================================================
# Setup
# =============================================================

mkdir -p "$WORK_DIR"
rm -f "$COOKIE_FILE"

echo "============================================================"
echo ">> TA Server Setup"
echo "   TAS_URL   : $TAS_URL"
echo "   Admin ID  : $TA_ADMIN_ID"
echo "   TA Name   : $TA_NAME"
echo "   Server URL: $TA_SERVER_URL"
echo "   Cert DN   : $TA_CERT_DN"
echo "   Work Dir  : $WORK_DIR"
echo "============================================================"
echo ""

# =============================================================
# Step 1: Health Check
# =============================================================

wait_for_health
echo ""

# =============================================================
# Step 2: Admin Login (Initial Password)
# =============================================================

post_json "Admin Login" "/login" \
    "{\"loginId\": \"$TA_ADMIN_ID\", \"loginPassword\": \"$TA_ADMIN_INITIAL_PASSWORD_HASH\"}"
LOGIN_RESPONSE="$RESPONSE"
save_artifact "01-login.json" "$LOGIN_RESPONSE"

REQUIRE_RESET=$(echo "$LOGIN_RESPONSE" | jq -r '.requirePasswordReset // false')

# =============================================================
# Step 3: Password Reset (if required)
# =============================================================

if [ "$REQUIRE_RESET" = "true" ]; then
    post_json "Reset Password" "/admins/reset-password" \
        "{\"loginId\": \"$TA_ADMIN_ID\", \"oldPassword\": \"$TA_ADMIN_INITIAL_PASSWORD_HASH\", \"newPassword\": \"$TA_ADMIN_NEW_PASSWORD_HASH\"}"
    save_artifact "02-reset-password.json" "$RESPONSE"

    # Step 4: Re-login with new password
    post_json "Re-login" "/login" \
        "{\"loginId\": \"$TA_ADMIN_ID\", \"loginPassword\": \"$TA_ADMIN_NEW_PASSWORD_HASH\"}"
    save_artifact "03-relogin.json" "$RESPONSE"
else
    echo ">> [Reset Password] Skipped (requirePasswordReset=false)"
fi
echo ""

# =============================================================
# Step 5: Validate TA Secret
# =============================================================

post_json "Validate TA Secret" "/ta/validate-ta-secret" \
    "{\"secret\": \"$TA_SECRET\"}"
echo ""

# =============================================================
# Step 6: Register TA Info
# =============================================================

post_json "Register TA Info" "/ta/register-ta-info" \
    "{\"name\": \"$TA_NAME\", \"serverUrl\": \"$TA_SERVER_URL\"}"
save_artifact "04-ta-info.json" "$RESPONSE"
echo ""

# =============================================================
# Step 7: Generate DID (Auto)
# =============================================================

post_json "Generate DID" "/ta/generate-did-auto" "{}"
DID_DOC="$RESPONSE"
save_artifact "05-did-document.json" "$DID_DOC"

# Serialize DID Document as a JSON string value
DID_DOC_STRING=$(echo "$DID_DOC" | jq -c '.' | jq -Rs '.')
echo ""

# =============================================================
# Step 8: Register TA DID
# =============================================================

post_json "Register TA DID" "/ta/register-ta-did" \
    "{\"didDocument\": $DID_DOC_STRING}"
echo ""

# =============================================================
# Step 9: Generate Certificate
# =============================================================

DN_ESCAPED=$(echo "$TA_CERT_DN" | jq -Rs '.')
post_json "Generate Certificate" "/ta/generate-certificate" \
    "{\"dn\": $DN_ESCAPED}"
CERT_VC="$RESPONSE"
save_artifact "06-certificate-vc.json" "$CERT_VC"

# Serialize Certificate VC as a JSON string value
CERT_VC_STRING=$(echo "$CERT_VC" | jq -c '.' | jq -Rs '.')
echo ""

# =============================================================
# Step 10: Register Certificate
# =============================================================

post_json "Register Certificate" "/ta/certificate" \
    "{\"certificate\": $CERT_VC_STRING}"
echo ""

# =============================================================
# Step 11: Verify Final State
# =============================================================

get_json "Verify TA State" "/ta/info"
FINAL_STATE="$RESPONSE"
save_artifact "07-ta-final-state.json" "$FINAL_STATE"

assert_field "$FINAL_STATE" '.status' "COMPLETED"

TA_DID=$(echo "$FINAL_STATE" | jq -r '.did')
if [ -z "$TA_DID" ] || [ "$TA_DID" = "null" ]; then
    LAST_RESPONSE="$FINAL_STATE"
    fail "TA DID is missing in final state"
fi

ACTUAL_NAME=$(echo "$FINAL_STATE" | jq -r '.name')
ACTUAL_URL=$(echo "$FINAL_STATE" | jq -r '.serverUrl')

echo ""
echo "============================================================"
echo ">> TA Server Setup COMPLETED"
echo "   TA DID     : $TA_DID"
echo "   Name       : $ACTUAL_NAME"
echo "   Server URL : $ACTUAL_URL"
echo "   Artifacts  : $WORK_DIR"
echo "============================================================"
