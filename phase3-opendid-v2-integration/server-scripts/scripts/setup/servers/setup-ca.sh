#!/bin/bash
set -euo pipefail

# =============================================================
# setup-ca.sh - OpenDID PoC CA Server Setup Automation
# =============================================================
# Prerequisites:
#   - TA server setup must be completed
#   - Setup Reset must have been performed beforehand
#
# Usage:
#   ./setup-ca.sh [options]
#
# Options:
#   --ip <ip>                 Server IP address
#   --cas-url <url>           CA server base URL (default: derived from --ip)
#   --tas-url <url>           TA server base URL (default: derived from --ip)
#   --admin-id <id>           CA admin login ID (default: admin@opendid.omnione.net)
#   --ta-admin-id <id>        TA admin login ID (default: admin@opendid.omnione.net)
#   --ca-name <name>          CA name (default: "OpenDID CA")
#   --work-dir <dir>          Artifact output directory
#   -h, --help                Show this help
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# --- Defaults ---
_LOCAL_IP=""
CAS_URL=""
TAS_URL=""
CAS_ADMIN_ID="admin@opendid.omnione.net"
TA_ADMIN_ID="admin@opendid.omnione.net"
CA_NAME="OpenDID CA"
WORK_DIR="$PROJECT_ROOT/logs/ca-setup"
LOCK_DIR=""

# 비밀번호 고정: "password"의 SHA-256 해시
PASSWORD_HASH="5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"

# --- Parse Arguments ---
while [[ $# -gt 0 ]]; do
    case "$1" in
        --ip)           _LOCAL_IP="$2";    shift 2 ;;
        --cas-url)      CAS_URL="$2";      shift 2 ;;
        --tas-url)      TAS_URL="$2";      shift 2 ;;
        --admin-id)     CAS_ADMIN_ID="$2"; shift 2 ;;
        --ta-admin-id)  TA_ADMIN_ID="$2";  shift 2 ;;
        --ca-name)      CA_NAME="$2";      shift 2 ;;
        --work-dir)     WORK_DIR="$2";     shift 2 ;;
        --lock-dir)     LOCK_DIR="$2";     shift 2 ;;
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

# --- Resolve IP / URLs ---
if [ -z "$_LOCAL_IP" ] && [ -z "$CAS_URL" ]; then
    read -rp "Server IP: " _LOCAL_IP
fi

if [ -z "$CAS_URL" ]; then
    CAS_URL="http://${_LOCAL_IP}:8094/cas"
fi

if [ -z "$TAS_URL" ]; then
    TAS_URL="http://${_LOCAL_IP}:8090/tas"
fi

CAS_SERVER_URL="$CAS_URL"

ADMIN_BASE="$CAS_URL/admin/v1"
TAS_ADMIN_BASE="$TAS_URL/admin/v1"

CAS_COOKIE="$WORK_DIR/ca-cookies.txt"
TA_COOKIE="$WORK_DIR/ta-cookies.txt"

# =============================================================
# Utility Functions
# =============================================================

# Step 11 직렬화 뮤텍스 (mkdir은 POSIX 원자적 연산)
acquire_lock() {
    [ -z "$LOCK_DIR" ] && return
    local waited=0
    while ! mkdir "$LOCK_DIR" 2>/dev/null; do
        if [ $((waited % 5)) -eq 0 ]; then
            echo "   [waiting for enroll lock... ${waited}s]"
        fi
        sleep 1
        waited=$((waited + 1))
    done
}

release_lock() {
    [ -z "$LOCK_DIR" ] && return
    rmdir "$LOCK_DIR" 2>/dev/null || true
}

cleanup() {
    release_lock
}
trap cleanup EXIT

fail() {
    echo ""
    echo "================================================================"
    echo "  FAILED: $1"
    echo "  Step    : ${CURRENT_STEP:-unknown}"
    echo "  URL     : ${LAST_URL:-unknown}"
    if [ -n "${LAST_BODY:-}" ]; then
        echo "  Request : $LAST_BODY"
    fi
    if [ -n "${LAST_STATUS:-}" ]; then
        echo "  HTTP    : $LAST_STATUS"
    fi
    if [ -n "${LAST_RESPONSE:-}" ]; then
        echo "  Response: $LAST_RESPONSE"
    fi
    echo "================================================================"
    exit 1
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

# post_json <step_label> <base> <path> <body> <cookie_file>
post_json() {
    CURRENT_STEP="$1"
    local base="$2"
    local path="$3"
    local body="$4"
    local cookie_file="$5"
    LAST_URL="${base}${path}"
    LAST_BODY="$body"

    echo ">> [$CURRENT_STEP]"

    local tmp_file
    tmp_file=$(mktemp)
    local http_code
    http_code=$(curl -s -o "$tmp_file" -w "%{http_code}" \
        -X POST "$LAST_URL" \
        -H "Content-Type: application/json" \
        -b "$cookie_file" -c "$cookie_file" \
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

# get_json <step_label> <base> <path> <cookie_file>
get_json() {
    CURRENT_STEP="$1"
    local base="$2"
    local path="$3"
    local cookie_file="$4"
    LAST_URL="${base}${path}"
    LAST_BODY=""

    echo ">> [$CURRENT_STEP]"

    local tmp_file
    tmp_file=$(mktemp)
    local http_code
    http_code=$(curl -s -o "$tmp_file" -w "%{http_code}" \
        -X GET "$LAST_URL" \
        -b "$cookie_file" -c "$cookie_file")
    LAST_RESPONSE=$(cat "$tmp_file")
    rm -f "$tmp_file"
    LAST_STATUS="$http_code"

    if [[ "$http_code" != 2* ]]; then
        fail "HTTP $http_code"
    fi

    RESPONSE="$LAST_RESPONSE"
    echo "   [OK]"
}

# =============================================================
# Preflight
# =============================================================

command -v jq &>/dev/null || { echo "ERROR: 'jq' is required but not installed."; exit 1; }

mkdir -p "$WORK_DIR"

echo ""
echo "============================================================="
echo "  CA Server Setup"
echo "============================================================="
echo "  CAS_URL  : $CAS_URL"
echo "  TAS_URL  : $TAS_URL"
echo "  ADMIN_ID : $CAS_ADMIN_ID"
echo "  WORK_DIR : $WORK_DIR"
echo "============================================================="
echo ""

# =============================================================
# Step 1: CA admin 로그인
# =============================================================

post_json "Step 1: CA admin login" \
    "$ADMIN_BASE" "/login" \
    "{\"loginId\": \"${CAS_ADMIN_ID}\", \"loginPassword\": \"${PASSWORD_HASH}\"}" \
    "$CAS_COOKIE"

save_artifact "01-login.json" "$RESPONSE"

REQUIRE_RESET=$(echo "$RESPONSE" | jq -r '.requirePasswordReset // false')

# =============================================================
# Step 2: 비밀번호 변경 (필요한 경우)
# =============================================================

if [[ "$REQUIRE_RESET" == "true" ]]; then
    post_json "Step 2: Reset password" \
        "$ADMIN_BASE" "/admins/reset-password" \
        "{\"loginId\": \"${CAS_ADMIN_ID}\", \"oldPassword\": \"${PASSWORD_HASH}\", \"newPassword\": \"${PASSWORD_HASH}\"}" \
        "$CAS_COOKIE"

    save_artifact "02-reset-password.json" "$RESPONSE"

    post_json "Step 3: Re-login" \
        "$ADMIN_BASE" "/login" \
        "{\"loginId\": \"${CAS_ADMIN_ID}\", \"loginPassword\": \"${PASSWORD_HASH}\"}" \
        "$CAS_COOKIE"

    save_artifact "03-relogin.json" "$RESPONSE"
else
    echo ">> [Step 2: Reset password] skipped (requirePasswordReset=false)"
fi

# =============================================================
# Step 4: CA 정보 등록
# =============================================================

post_json "Step 4: Register CA info" \
    "$ADMIN_BASE" "/ca/register-ca-info" \
    "{\"name\": \"${CA_NAME}\", \"serverUrl\": \"${CAS_SERVER_URL}\"}" \
    "$CAS_COOKIE"

save_artifact "04-register-ca-info.json" "$RESPONSE"
assert_field "$RESPONSE" ".status" "DID_DOCUMENT_REQUIRED"

# =============================================================
# Step 5: DID 자동 생성
# =============================================================

post_json "Step 5: Generate DID auto" \
    "$ADMIN_BASE" "/ca/generate-did-auto" \
    "" \
    "$CAS_COOKIE"

save_artifact "05-did-document.json" "$RESPONSE"
DID_DOC_STR=$(echo "$RESPONSE" | jq -c '.')

# =============================================================
# Step 6: TA에 DID 등록 요청
# =============================================================

REGISTER_DID_BODY=$(jq -n --arg doc "$DID_DOC_STR" '{"didDocument": $doc}')

post_json "Step 6: Register DID to TA" \
    "$ADMIN_BASE" "/ca/register-did" \
    "$REGISTER_DID_BODY" \
    "$CAS_COOKIE"

save_artifact "06-register-did.json" "$RESPONSE"

# =============================================================
# Step 7: TA admin 로그인
# =============================================================

post_json "Step 7: TA admin login" \
    "$TAS_ADMIN_BASE" "/login" \
    "{\"loginId\": \"${TA_ADMIN_ID}\", \"loginPassword\": \"${PASSWORD_HASH}\"}" \
    "$TA_COOKIE"

save_artifact "07-ta-login.json" "$RESPONSE"

# =============================================================
# Step 8: CA Entity ID 조회
# =============================================================

get_json "Step 8: Get entity list from TA" \
    "$TAS_ADMIN_BASE" "/entities/list" \
    "$TA_COOKIE"

save_artifact "08-entity-list.json" "$RESPONSE"

ENTITY_ID=$(echo "$RESPONSE" | jq -r '.content[] | select(.status == "DID_DOCUMENT_REQUIRED" and .role == "APP_PROVIDER") | .id' | head -n 1)
if [ -z "$ENTITY_ID" ] || [ "$ENTITY_ID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "CA Entity ID not found in TA entity list (no DID_DOCUMENT_REQUIRED + APP_PROVIDER entity)"
fi

echo "   Entity ID: $ENTITY_ID"

# =============================================================
# Step 9: TA에서 CA DID 승인
# =============================================================

post_json "Step 9: Approve entity DID in TA" \
    "$TAS_ADMIN_BASE" "/entities/approve-did" \
    "{\"entityId\": ${ENTITY_ID}}" \
    "$TA_COOKIE"

save_artifact "09-approve-did.json" "$RESPONSE"

# =============================================================
# Step 10: 승인 상태 확인
# =============================================================

get_json "Step 10: Check request status" \
    "$ADMIN_BASE" "/ca/request-status" \
    "$CAS_COOKIE"

save_artifact "10-request-status.json" "$RESPONSE"
assert_field "$RESPONSE" ".status" "CERTIFICATE_VC_REQUIRED"

# =============================================================
# Step 11: Entity 등록 (뮤텍스 보호 - TA 동시 서명 충돌 방지)
# =============================================================

acquire_lock

post_json "Step 11: Enroll entity" \
    "$ADMIN_BASE" "/ca/request-enroll-entity" \
    "" \
    "$CAS_COOKIE"

release_lock

save_artifact "11-enroll-entity.json" "$RESPONSE"

# =============================================================
# Step 12: 최종 상태 검증
# =============================================================

get_json "Step 12: Verify final state" \
    "$ADMIN_BASE" "/ca/info" \
    "$CAS_COOKIE"

save_artifact "12-ca-info.json" "$RESPONSE"

assert_field "$RESPONSE" ".status" "ACTIVATE"

ACTUAL_DID=$(echo "$RESPONSE" | jq -r '.did')
if [ -z "$ACTUAL_DID" ] || [ "$ACTUAL_DID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "DID is missing in final state"
fi

echo ""
echo "============================================================="
echo "  CA Setup Complete"
echo "  DID   : $ACTUAL_DID"
echo "  Name  : $(echo "$RESPONSE" | jq -r '.name')"
echo "  Status: ACTIVATE"
echo "============================================================="
