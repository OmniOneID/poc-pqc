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

# Create VP filter + profile + policy for given claim count.
# Uses outer scope: PAYLOAD_ID, PROCESS_DB_ID, VERIFIER_DID, VERIFIER_CERT_URL, VERIFIER_NAME, VERIFIER_SERVER_URL
create_vp_policy() {
    local claim_count=$1
    local step_base=$2
    local tag="c${claim_count}"
    local filter_title="IDCard VC Filter (${claim_count} claims)"
    local profile_title="IDCard VC Verification (${claim_count} claims)"
    local policy_title="IDCard VC Policy (${claim_count} claims)"
    local schema_id="vc.schema.perf.claims.${claim_count}"

    local filter_claims_json
    filter_claims_json=$(build_claims_array "perf.claims.${claim_count}" "$claim_count")

    local filter_body
    filter_body=$(jq -n \
        --arg title "$filter_title" \
        --arg id "$schema_id" \
        --argjson claims "$filter_claims_json" \
        '{
          "title": $title,
          "id": $id,
          "type": "OsdSchemaCredential",
          "requiredClaims": $claims,
          "displayClaims": $claims,
          "allowedIssuers": ["did:omn:issuer"],
          "presentAll": false
        }')

    post_json "Verifier-${step_base}: Register VP filter (${claim_count} claims)" \
        "$VERIFIER_ADMIN_BASE" "/filters" \
        "$filter_body" \
        "$VERIFIER_COOKIE"
    save_artifact "verifier-${tag}-filter.json" "$RESPONSE"

    get_json "Verifier-$((step_base+1)): Query filter list (${claim_count} claims)" \
        "$VERIFIER_ADMIN_BASE" "/filters?page=0&size=100" \
        "$VERIFIER_COOKIE"
    save_artifact "verifier-${tag}-filter-list.json" "$RESPONSE"

    local filter_db_id
    filter_db_id=$(echo "$RESPONSE" | jq -r ".content[] | select(.title == \"${filter_title}\") | .filterId" | head -n 1)
    if [ -z "$filter_db_id" ] || [ "$filter_db_id" = "null" ]; then
        LAST_RESPONSE="$RESPONSE"
        fail "Filter '${filter_title}' not found after creation"
    fi
    echo "   Filter DB ID: $filter_db_id"

    local profile_body
    profile_body=$(jq -n \
        --arg did "$VERIFIER_DID" \
        --arg certVcRef "$VERIFIER_CERT_URL" \
        --arg name "$VERIFIER_NAME" \
        --arg ref "$VERIFIER_SERVER_URL" \
        --arg title "$profile_title" \
        --arg desc "VP verification profile for ${claim_count} claims" \
        --argjson processId "$PROCESS_DB_ID" \
        --argjson filterId "$filter_db_id" \
        '{
          "type": "VerifyProfile",
          "title": $title,
          "description": $desc,
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

    post_json "Verifier-$((step_base+2)): Register VP profile (${claim_count} claims)" \
        "$VERIFIER_ADMIN_BASE" "/profiles" \
        "$profile_body" \
        "$VERIFIER_COOKIE"
    save_artifact "verifier-${tag}-profile.json" "$RESPONSE"

    get_json "Verifier-$((step_base+3)): Query profile list (${claim_count} claims)" \
        "$VERIFIER_ADMIN_BASE" "/profiles?page=0&size=100" \
        "$VERIFIER_COOKIE"
    save_artifact "verifier-${tag}-profile-list.json" "$RESPONSE"

    local policy_profile_id
    policy_profile_id=$(echo "$RESPONSE" | jq -r ".content[] | select(.title == \"${profile_title}\") | .policyProfileId" | head -n 1)
    if [ -z "$policy_profile_id" ] || [ "$policy_profile_id" = "null" ]; then
        LAST_RESPONSE="$RESPONSE"
        fail "Profile '${profile_title}' not found after creation"
    fi
    echo "   Policy Profile ID: $policy_profile_id"

    local policy_body
    policy_body=$(jq -n \
        --arg payloadId "$PAYLOAD_ID" \
        --arg policyProfileId "$policy_profile_id" \
        --arg title "$policy_title" \
        '{
          "policyTitle": $title,
          "payloadId": $payloadId,
          "policyProfileId": $policyProfileId
        }')

    post_json "Verifier-$((step_base+4)): Register VP policy (${claim_count} claims)" \
        "$VERIFIER_ADMIN_BASE" "/policies?policyType=VP" \
        "$policy_body" \
        "$VERIFIER_COOKIE"
    save_artifact "verifier-${tag}-policy.json" "$RESPONSE"
}

# Build a JSON array of namespace items: field01..fieldNN
build_items_array() {
    local count=$1
    local arr="["
    local i
    for i in $(seq 1 "$count"); do
        local pad
        pad=$(printf "%02d" "$i")
        arr="${arr}{\"id\":\"field${pad}\",\"caption\":\"Field ${pad}\",\"type\":\"text\",\"format\":\"plain\"}"
        [ "$i" -lt "$count" ] && arr="${arr},"
    done
    echo "${arr}]"
}

# Build a JSON array of qualified claim strings: ns.field01..ns.fieldNN
build_claims_array() {
    local ns=$1
    local count=$2
    local arr="["
    local i
    for i in $(seq 1 "$count"); do
        local pad
        pad=$(printf "%02d" "$i")
        arr="${arr}\"${ns}.field${pad}\""
        [ "$i" -lt "$count" ] && arr="${arr},"
    done
    echo "${arr}]"
}

# Create one complete VC policy set: namespace + VC schema + issue profile
# Args: claim_count step_base
create_vc_policy() {
    local claim_count=$1
    local step_base=$2
    local ns_id="perf.claims.${claim_count}"
    local schema_id="vc.schema.perf.claims.${claim_count}"
    local plan_id="perf.claims.${claim_count}"
    local tag="c${claim_count}"

    local items_json
    items_json=$(build_items_array "$claim_count")

    local ns_body
    ns_body=$(jq -n \
        --arg id "$ns_id" \
        --arg name "Perf Claims ${claim_count}" \
        --arg ref "https://example.org/ns/perf/claims/${claim_count}" \
        --argjson items "$items_json" \
        '{"namespace": {"id": $id, "name": $name, "ref": $ref}, "items": $items}')

    post_json "Issuer-${step_base}: Create namespace (${claim_count} claims)" \
        "$ISSUER_ADMIN_BASE" "/namespaces" \
        "$ns_body" \
        "$ISSUER_COOKIE"
    save_artifact "issuer-${tag}-namespace.json" "$RESPONSE"

    get_json "Issuer-$((step_base+1)): Query namespace list (${claim_count} claims)" \
        "$ISSUER_ADMIN_BASE" "/namespaces?page=0&size=100" \
        "$ISSUER_COOKIE"
    save_artifact "issuer-${tag}-namespace-list.json" "$RESPONSE"

    local ns_db_id
    ns_db_id=$(echo "$RESPONSE" | jq -r ".content[] | select(.namespaceId == \"${ns_id}\") | .id" | head -n 1)
    if [ -z "$ns_db_id" ] || [ "$ns_db_id" = "null" ]; then
        LAST_RESPONSE="$RESPONSE"
        fail "Namespace '${ns_id}' not found after creation"
    fi
    echo "   Namespace DB ID: $ns_db_id"

    local schema_body
    schema_body=$(jq -n \
        --argjson nsId "$ns_db_id" \
        --arg schemaId "$schema_id" \
        --arg title "Perf Claims ${claim_count} VC" \
        --arg desc "Performance test VC with ${claim_count} claims" \
        '{
          "namespaces": [$nsId],
          "vcSchemaId": $schemaId,
          "title": $title,
          "description": $desc,
          "language": "ko",
          "version": "1.0"
        }')

    post_json "Issuer-$((step_base+2)): Create VC schema (${claim_count} claims)" \
        "$ISSUER_ADMIN_BASE" "/vc-schemas" \
        "$schema_body" \
        "$ISSUER_COOKIE"
    save_artifact "issuer-${tag}-vc-schema.json" "$RESPONSE"

    get_json "Issuer-$((step_base+3)): Query VC schema list (${claim_count} claims)" \
        "$ISSUER_ADMIN_BASE" "/vc-schemas?page=0&size=100" \
        "$ISSUER_COOKIE"
    save_artifact "issuer-${tag}-vc-schema-list.json" "$RESPONSE"

    local schema_db_id
    schema_db_id=$(echo "$RESPONSE" | jq -r ".content[] | select(.vcSchemaId == \"${schema_id}\") | .id" | head -n 1)
    if [ -z "$schema_db_id" ] || [ "$schema_db_id" = "null" ]; then
        LAST_RESPONSE="$RESPONSE"
        fail "VC Schema '${schema_id}' not found after creation"
    fi
    echo "   VC Schema DB ID: $schema_db_id"

    local profile_body
    profile_body=$(jq -n \
        --arg ip "$_LOCAL_IP" \
        --argjson schemaId "$schema_db_id" \
        --arg planId "$plan_id" \
        --arg title "Perf Claims ${claim_count} VC plan" \
        --arg desc "Performance test VC plan with ${claim_count} claims" \
        '{
          "vcPlanId": $planId,
          "title": $title,
          "description": $desc,
          "vcSchemaId": $schemaId,
          "language": "ko",
          "endpoints": [("http://" + $ip + ":8091")],
          "cipher": "AES-256-CBC",
          "curve": "Secp256r1",
          "padding": "PKCS5",
          "initiateType": "user_init",
          "tags": [($planId + ".init")],
          "zkpEnabled": false
        }')

    post_json "Issuer-$((step_base+4)): Create issue profile (${claim_count} claims)" \
        "$ISSUER_ADMIN_BASE" "/issue-profiles" \
        "$profile_body" \
        "$ISSUER_COOKIE"
    save_artifact "issuer-${tag}-issue-profile.json" "$RESPONSE"
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

echo ""
echo "=== TA: KYC Settings ==="

post_json "TA-1: Admin login" \
    "$TA_ADMIN_BASE" "/login" \
    "{\"loginId\": \"${TA_ADMIN_ID}\", \"loginPassword\": \"${PASSWORD_HASH}\"}" \
    "$TA_COOKIE"
save_artifact "ta-01-login.json" "$RESPONSE"

post_json "TA-2: Register KYC" \
    "$TA_ADMIN_BASE" "/kycs" \
    "{\"name\": \"CAS\", \"serverUrl\": \"http://${_LOCAL_IP}:8094/cas\"}" \
    "$TA_COOKIE"
save_artifact "ta-02-kyc.json" "$RESPONSE"

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

# --- Issuer-2~16: Create namespace / VC schema / issue profile for each claim count ---
create_vc_policy 5   2
create_vc_policy 50  7
create_vc_policy 100 12

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
      "endpoints": (["http://" + $ip + ":8092/verify"] | tojson)
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

# --- Verifier-5: Register VP Process (shared across all claim counts) ---
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

post_json "Verifier-5: Register VP process" \
    "$VERIFIER_ADMIN_BASE" "/processes" \
    "$PROCESS_BODY" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-05-process.json" "$RESPONSE"

# --- Verifier-6: Get Process DB ID ---
get_json "Verifier-6: Query process list" \
    "$VERIFIER_ADMIN_BASE" "/processes?page=0&size=100" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-06-process-list.json" "$RESPONSE"

PROCESS_DB_ID=$(echo "$RESPONSE" | jq -r '.content[] | select(.title == "Default Verification Process") | .id' | head -n 1)
if [ -z "$PROCESS_DB_ID" ] || [ "$PROCESS_DB_ID" = "null" ]; then
    LAST_RESPONSE="$RESPONSE"
    fail "Process 'Default Verification Process' not found after creation"
fi
echo "   Process DB ID: $PROCESS_DB_ID"

# --- Verifier-7~25: VP Filter + Profile + Policy (claim count: 5 / 50 / 100) ---
create_vp_policy 5   7
create_vp_policy 50  13
create_vp_policy 100 19

# --- Verifier-26: Query VP policy list to extract policyIds for build.gradle ---
get_json "Verifier-26: Query VP policy list" \
    "$VERIFIER_ADMIN_BASE" "/policies?policyType=VP&page=0&size=100" \
    "$VERIFIER_COOKIE"
save_artifact "verifier-26-vp-policy-list.json" "$RESPONSE"

VP_POLICY_ID_5=$(echo   "$RESPONSE" | jq -r '.content[] | select(.policyTitle == "IDCard VC Policy (5 claims)")   | .policyId' | head -n 1)
VP_POLICY_ID_50=$(echo  "$RESPONSE" | jq -r '.content[] | select(.policyTitle == "IDCard VC Policy (50 claims)")  | .policyId' | head -n 1)
VP_POLICY_ID_100=$(echo "$RESPONSE" | jq -r '.content[] | select(.policyTitle == "IDCard VC Policy (100 claims)") | .policyId' | head -n 1)

echo "   VP Policy IDs:"
echo "   perf.claims.5  : ${VP_POLICY_ID_5:-(not found — check verifier-26-vp-policy-list.json)}"
echo "   perf.claims.50 : ${VP_POLICY_ID_50:-(not found)}"
echo "   perf.claims.100: ${VP_POLICY_ID_100:-(not found)}"

echo ""
echo "============================================================="
echo "  Admin Policy Setup Complete"
echo "============================================================="
echo "  TA       : KYC registered (CAS at http://${_LOCAL_IP}:8094/cas)"
echo "  Issuer   : 3 namespaces / 3 VC schemas / 3 issue profiles registered"
echo "             vcPlanId: perf.claims.5 / perf.claims.50 / perf.claims.100"
echo "  Verifier : Service / Process / 3 VP Filters / 3 VP Profiles / 3 VP Policies"
echo "============================================================="
echo "  build.gradle 에 아래 VP Policy ID 를 입력하세요 (Scenario C 용):"
echo "    AUTO_TEST_VP_POLICY_ID_5   = \"${VP_POLICY_ID_5}\""
echo "    AUTO_TEST_VP_POLICY_ID_50  = \"${VP_POLICY_ID_50}\""
echo "    AUTO_TEST_VP_POLICY_ID_100 = \"${VP_POLICY_ID_100}\""
echo "============================================================="
