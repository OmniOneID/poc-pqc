#!/bin/bash
set -euo pipefail

# =============================================================
# setup-admin-policy.sh - OpenDID PQC Admin Policy Setup
# =============================================================
# Prerequisites:
#   - All entity servers (TA/CA/Issuer/Verifier) setup must be completed
#   - Each server status must be ACTIVATE
#
# Usage:
#   ./setup-admin-policy.sh [options]
#
# Options:
#   --ip <ip>                   Server IP address
#   --tas-url <url>             TA server base URL (default: derived from --ip)
#   --issuer-url <url>          Issuer server base URL (default: derived from --ip)
#   --verifier-url <url>        Verifier server base URL (default: derived from --ip)
#   --ta-admin-id <id>          TA admin login ID (default: admin@opendid.omnione.net)
#   --issuer-admin-id <id>      Issuer admin login ID (default: admin@opendid.omnione.net)
#   --verifier-admin-id <id>    Verifier admin login ID (default: admin@opendid.omnione.net)
#   --work-dir <dir>            Artifact output directory
#   -h, --help                  Show this help
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# --- Defaults ---
_LOCAL_IP=""
TAS_URL=""
ISSUER_URL=""
VERIFIER_URL=""
TA_ADMIN_ID="admin@opendid.omnione.net"
ISSUER_ADMIN_ID="admin@opendid.omnione.net"
VERIFIER_ADMIN_ID="admin@opendid.omnione.net"
WORK_DIR="$PROJECT_ROOT/logs/admin-policy-setup"

# 비밀번호 고정: "password"의 SHA-256 해시
PASSWORD_HASH="5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"

# --- Parse Arguments ---
while [[ $# -gt 0 ]]; do
    case "$1" in
        --ip)               _LOCAL_IP="$2";           shift 2 ;;
        --tas-url)          TAS_URL="$2";              shift 2 ;;
        --issuer-url)       ISSUER_URL="$2";           shift 2 ;;
        --verifier-url)     VERIFIER_URL="$2";         shift 2 ;;
        --ta-admin-id)      TA_ADMIN_ID="$2";          shift 2 ;;
        --issuer-admin-id)  ISSUER_ADMIN_ID="$2";      shift 2 ;;
        --verifier-admin-id) VERIFIER_ADMIN_ID="$2";  shift 2 ;;
        --work-dir)         WORK_DIR="$2";             shift 2 ;;
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
if [ -z "$_LOCAL_IP" ]; then
    read -rp "Server IP: " _LOCAL_IP
fi

[ -z "$TAS_URL" ]      && TAS_URL="http://${_LOCAL_IP}:8090/tas"
[ -z "$ISSUER_URL" ]   && ISSUER_URL="http://${_LOCAL_IP}:8091/issuer"
[ -z "$VERIFIER_URL" ] && VERIFIER_URL="http://${_LOCAL_IP}:8092/verifier"

TA_ADMIN_BASE="$TAS_URL/admin/v1"
ISSUER_ADMIN_BASE="$ISSUER_URL/admin/v1"
VERIFIER_ADMIN_BASE="$VERIFIER_URL/admin/v1"

TA_COOKIE="$WORK_DIR/ta-cookies.txt"
ISSUER_COOKIE="$WORK_DIR/issuer-cookies.txt"
VERIFIER_COOKIE="$WORK_DIR/verifier-cookies.txt"

# =============================================================
# Utility Functions
# =============================================================

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
echo "  Admin Policy Setup"
echo "============================================================="
echo "  TAS_URL      : $TAS_URL"
echo "  ISSUER_URL   : $ISSUER_URL"
echo "  VERIFIER_URL : $VERIFIER_URL"
echo "  WORK_DIR     : $WORK_DIR"
echo "============================================================="
echo ""

# =============================================================
# Section 1: TA - KYC Settings
# =============================================================

# echo ""
# echo "=== TA: KYC Settings ==="

# post_json "TA-1: Admin login" \
#     "$TA_ADMIN_BASE" "/login" \
#     "{\"loginId\": \"${TA_ADMIN_ID}\", \"loginPassword\": \"${PASSWORD_HASH}\"}" \
#     "$TA_COOKIE"
# save_artifact "ta-01-login.json" "$RESPONSE"

# post_json "TA-2: Register KYC" \
#     "$TA_ADMIN_BASE" "/kycs" \
#     "{\"name\": \"CAS\", \"serverUrl\": \"http://${_LOCAL_IP}:8094/cas\"}" \
#     "$TA_COOKIE"
# save_artifact "ta-02-kyc.json" "$RESPONSE"

# =============================================================
# Section 2: Issuer - VC Policy Settings
# =============================================================

echo ""
echo "=== Issuer: VC Policy Settings ==="

# --- Issuer-1: Login ---
post_json "Issuer-1: Admin login" \
    "$ISSUER_ADMIN_BASE" "/login" \
    "{\"loginId\": \"${ISSUER_ADMIN_ID}\", \"loginPassword\": \"${PASSWORD_HASH}\"}" \
    "$ISSUER_COOKIE"
save_artifact "issuer-01-login.json" "$RESPONSE"

# --- Issuer-2: Create Namespace ---
NAMESPACE_BODY=$(cat <<'BODY'
{
  "namespace": {
    "id": "person.basic",
    "name": "Basic Personal Information",
    "ref": "https://example.org/ns/person/basic"
  },
  "items": [
    {"id": "name",   "caption": "Full Name",  "type": "text", "format": "plain"},
    {"id": "birth",  "caption": "Birth Date", "type": "text", "format": "plain"},
    {"id": "gender", "caption": "Gender",     "type": "text", "format": "plain"}
  ]
}
BODY
)

post_json "Issuer-2: Create namespace" \
    "$ISSUER_ADMIN_BASE" "/namespaces" \
    "$NAMESPACE_BODY" \
    "$ISSUER_COOKIE"
save_artifact "issuer-02-namespace.json" "$RESPONSE"

# --- Issuer-3: Get Namespace DB ID ---
get_json "Issuer-3: Query namespace list" \
    "$ISSUER_ADMIN_BASE" "/namespaces?page=0&size=100" \
    "$ISSUER_COOKIE"
save_artifact "issuer-03-namespace-list.json" "$RESPONSE"

NAMESPACE_DB_ID=$(echo "$RESPONSE" | jq -r '.content[] | select(.namespaceId == "person.basic") | .id' | head -n 1)
if [ -z "$NAMESPACE_DB_ID" ] || [ "$NAMESPACE_DB_ID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "Namespace 'person.basic' not found after creation"
fi
echo "   Namespace DB ID: $NAMESPACE_DB_ID"

# --- Issuer-4: Create VC Schema ---
VC_SCHEMA_BODY=$(jq -n \
    --argjson nsId "$NAMESPACE_DB_ID" \
    '{
      "namespaces": [$nsId],
      "vcSchemaId": "vc.schema.person.basic",
      "title": "Basic Person VC",
      "description": "Basic Identity VC",
      "language": "ko",
      "version": "1.0"
    }')

post_json "Issuer-4: Create VC schema" \
    "$ISSUER_ADMIN_BASE" "/vc-schemas" \
    "$VC_SCHEMA_BODY" \
    "$ISSUER_COOKIE"
save_artifact "issuer-04-vc-schema.json" "$RESPONSE"

# --- Issuer-5: Get VC Schema DB ID ---
get_json "Issuer-5: Query VC schema list" \
    "$ISSUER_ADMIN_BASE" "/vc-schemas?page=0&size=100" \
    "$ISSUER_COOKIE"
save_artifact "issuer-05-vc-schema-list.json" "$RESPONSE"

VC_SCHEMA_DB_ID=$(echo "$RESPONSE" | jq -r '.content[] | select(.vcSchemaId == "vc.schema.person.basic") | .id' | head -n 1)
if [ -z "$VC_SCHEMA_DB_ID" ] || [ "$VC_SCHEMA_DB_ID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "VC Schema 'vc.schema.person.basic' not found after creation"
fi
echo "   VC Schema DB ID: $VC_SCHEMA_DB_ID"

# --- Issuer-6: Create Issue Profile ---
ISSUE_PROFILE_BODY=$(jq -n \
    --arg ip "$_LOCAL_IP" \
    --argjson schemaId "$VC_SCHEMA_DB_ID" \
    '{
      "vcPlanId": "idcard.issuer-vc",
      "title": "ID Card VC plan",
      "description": "ID Card VC plan",
      "vcSchemaId": $schemaId,
      "language": "ko",
      "endpoints": [("http://" + $ip + ":8091")],
      "cipher": "AES-256-CBC",
      "curve": "Secp256r1",
      "padding": "PKCS5",
      "initiateType": "user_init",
      "tags": ["idcard.issuer.init"],
      "zkpEnabled": false
    }')

post_json "Issuer-6: Create issue profile" \
    "$ISSUER_ADMIN_BASE" "/issue-profiles" \
    "$ISSUE_PROFILE_BODY" \
    "$ISSUER_COOKIE"
save_artifact "issuer-06-issue-profile.json" "$RESPONSE"

# =============================================================
# Section 3: Verifier - VP Policy Settings
# =============================================================

echo ""
echo "=== Verifier: VP Policy Settings ==="

# --- Verifier-1: Login ---
post_json "Verifier-1: Admin login" \
    "$VERIFIER_ADMIN_BASE" "/login" \
    "{\"loginId\": \"${VERIFIER_ADMIN_ID}\", \"loginPassword\": \"${PASSWORD_HASH}\"}" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-01-login.json" "$RESPONSE"

# --- Verifier-2: Get Verifier Info ---
get_json "Verifier-2: Get verifier info" \
    "$VERIFIER_ADMIN_BASE" "/info" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-02-info.json" "$RESPONSE"

VERIFIER_DID=$(echo "$RESPONSE" | jq -r '.did')
VERIFIER_NAME=$(echo "$RESPONSE" | jq -r '.name')
VERIFIER_CERT_URL=$(echo "$RESPONSE" | jq -r '.certificateUrl')
VERIFIER_SERVER_URL=$(echo "$RESPONSE" | jq -r '.serverUrl')

if [ -z "$VERIFIER_DID" ] || [ "$VERIFIER_DID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "Verifier DID not found. Has Verifier entity setup been completed?"
fi
echo "   Verifier DID : $VERIFIER_DID"
echo "   Verifier Name: $VERIFIER_NAME"

# --- Verifier-3: Register Service (Payload) ---
PAYLOAD_BODY=$(jq -n \
    --arg ip "$_LOCAL_IP" \
    '{
      "payloadId": "idcard.vc.service",
      "service": "idcard.vc.service",
      "device": "WEB",
      "locked": false,
      "mode": "Direct",
      "validSecond": 180,
      "offerType": "VerifyOffer",
      "endpoints": [("http://" + $ip + ":8092/verify")]
    }')

post_json "Verifier-3: Register service (payload)" \
    "$VERIFIER_ADMIN_BASE" "/payloads" \
    "$PAYLOAD_BODY" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-03-payload.json" "$RESPONSE"

# --- Verifier-4: Get Payload ID ---
get_json "Verifier-4: Query payload list" \
    "$VERIFIER_ADMIN_BASE" "/payloads?page=0&size=100" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-04-payload-list.json" "$RESPONSE"

PAYLOAD_ID=$(echo "$RESPONSE" | jq -r '.content[] | select(.service == "idcard.vc.service") | .payloadId' | head -n 1)
if [ -z "$PAYLOAD_ID" ] || [ "$PAYLOAD_ID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "Payload 'idcard.vc.service' not found after creation"
fi
echo "   Payload ID: $PAYLOAD_ID"

# --- Verifier-5: Register VP Filter ---
FILTER_BODY=$(jq -n \
    --arg ip "$_LOCAL_IP" \
    '{
      "title": "IDCard VC Filter",
      "id": ("http://" + $ip + ":8091/issuer/api/v1/vc/vcschema?name=vc.schema.person.basic"),
      "type": "OsdSchemaCredential",
      "requiredClaims": ["person.basic.name", "person.basic.birth", "person.basic.gender"],
      "displayClaims":  ["person.basic.name", "person.basic.birth", "person.basic.gender"],
      "allowedIssuers": ["did:omn:issuer"],
      "presentAll": false
    }')

post_json "Verifier-5: Register VP filter" \
    "$VERIFIER_ADMIN_BASE" "/filters" \
    "$FILTER_BODY" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-05-filter.json" "$RESPONSE"

# --- Verifier-6: Get Filter DB ID ---
get_json "Verifier-6: Query filter list" \
    "$VERIFIER_ADMIN_BASE" "/filters?page=0&size=100" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-06-filter-list.json" "$RESPONSE"

FILTER_DB_ID=$(echo "$RESPONSE" | jq -r '.content[] | select(.title == "IDCard VC Filter") | .filterId' | head -n 1)
if [ -z "$FILTER_DB_ID" ] || [ "$FILTER_DB_ID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "Filter 'IDCard VC Filter' not found after creation"
fi
echo "   Filter DB ID: $FILTER_DB_ID"

# --- Verifier-7: Register VP Process ---
PROCESS_BODY=$(jq -n \
    --arg ip "$_LOCAL_IP" \
    '{
      "title": "Default Verification Process",
      "authType": 6,
      "reqE2e": {
        "curve": "Secp256r1",
        "cipher": "AES-256-CBC",
        "padding": "PKCS5"
      },
      "endpoints": [("http://" + $ip + ":8092/verifier")]
    }')

post_json "Verifier-7: Register VP process" \
    "$VERIFIER_ADMIN_BASE" "/processes" \
    "$PROCESS_BODY" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-07-process.json" "$RESPONSE"

# --- Verifier-8: Get Process DB ID ---
get_json "Verifier-8: Query process list" \
    "$VERIFIER_ADMIN_BASE" "/processes?page=0&size=100" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-08-process-list.json" "$RESPONSE"

PROCESS_DB_ID=$(echo "$RESPONSE" | jq -r '.content[] | select(.title == "Default Verification Process") | .id' | head -n 1)
if [ -z "$PROCESS_DB_ID" ] || [ "$PROCESS_DB_ID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "Process 'Default Verification Process' not found after creation"
fi
echo "   Process DB ID: $PROCESS_DB_ID"

# --- Verifier-9: Register VP Profile ---
PROFILE_BODY=$(jq -n \
    --arg did "$VERIFIER_DID" \
    --arg certVcRef "$VERIFIER_CERT_URL" \
    --arg name "$VERIFIER_NAME" \
    --arg ref "$VERIFIER_SERVER_URL" \
    --argjson processId "$PROCESS_DB_ID" \
    --argjson filterId "$FILTER_DB_ID" \
    '{
      "type": "VerifyProfile",
      "title": "IDCard VC Verification",
      "description": "VP verification profile for ID Card VC",
      "encoding": "UTF-8",
      "language": "ko",
      "verifier": {
        "did": $did,
        "certVcRef": $certVcRef,
        "name": $name,
        "ref": $ref
      },
      "processId": $processId,
      "filterId": $filterId
    }')

post_json "Verifier-9: Register VP profile" \
    "$VERIFIER_ADMIN_BASE" "/profiles" \
    "$PROFILE_BODY" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-09-profile.json" "$RESPONSE"

# --- Verifier-10: Get Profile policyProfileId ---
get_json "Verifier-10: Query profile list" \
    "$VERIFIER_ADMIN_BASE" "/profiles?page=0&size=100" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-10-profile-list.json" "$RESPONSE"

POLICY_PROFILE_ID=$(echo "$RESPONSE" | jq -r '.content[] | select(.title == "IDCard VC Verification") | .policyProfileId' | head -n 1)
if [ -z "$POLICY_PROFILE_ID" ] || [ "$POLICY_PROFILE_ID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "Profile 'IDCard VC Verification' not found after creation"
fi
echo "   Policy Profile ID: $POLICY_PROFILE_ID"

# --- Verifier-11: Register VP Policy ---
POLICY_BODY=$(jq -n \
    --arg payloadId "$PAYLOAD_ID" \
    --arg policyProfileId "$POLICY_PROFILE_ID" \
    '{
      "policyTitle": "IDCard VC Policy",
      "payloadId": $payloadId,
      "policyProfileId": $policyProfileId
    }')

post_json "Verifier-11: Register VP policy" \
    "$VERIFIER_ADMIN_BASE" "/policies?policyType=VP" \
    "$POLICY_BODY" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-11-policy.json" "$RESPONSE"

echo ""
echo "============================================================="
echo "  Admin Policy Setup Complete"
echo "============================================================="
echo "  TA       : KYC registered (CAS at http://${_LOCAL_IP}:8094/cas)"
echo "  Issuer   : Namespace / VC Schema / Issue Profile registered"
echo "  Verifier : Service / Filter / Process / Profile / Policy registered"
echo "============================================================="
