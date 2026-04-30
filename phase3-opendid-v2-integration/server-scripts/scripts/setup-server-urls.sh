#!/bin/bash
set -euo pipefail

# =============================================================
# setup-server-urls.sh - Multi-Machine Server URL Configuration
# =============================================================
# 서버가 여러 장비에 분산 배포된 경우 사용합니다.
# 단일 장비(IP 동일)인 경우: scripts/config/update-configs.sh
#
# 입력: 각 서버가 실행 중인 장비의 IP
# 저장: .server-urls-config (재실행 시 재사용)
# 적용:
#   - tas.url, lss.url   → 의존 서비스 URL
#   - springdoc.server.url → Swagger UI 주소 (자기 자신)
#   - issuer.url          → proxy 서버
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIGS_DIR="$PROJECT_ROOT/configs"
URL_CONFIG_FILE="$PROJECT_ROOT/.server-urls-config"

echo "============================================================"
echo ">> Multi-Machine Server URL Configuration"
echo "============================================================"
echo ""
echo "  서버가 여러 장비에 분산된 경우 사용합니다."
echo "  단일 장비(IP 동일)인 경우: scripts/config/update-configs.sh"
echo ""

# Load existing config
TAS_IP="" LSS_IP="" CAS_IP="" ISSUER_IP="" VERIFIER_IP="" WALLET_IP="" OP_IP="" API_IP=""
if [ -f "$URL_CONFIG_FILE" ]; then
    source "$URL_CONFIG_FILE"
    echo ">> 현재 저장된 IP 설정:"
    printf "   %-12s %s\n" "TAS:"      "${TAS_IP:-미설정}"
    printf "   %-12s %s\n" "LSS:"      "${LSS_IP:-미설정}"
    printf "   %-12s %s\n" "CAS:"      "${CAS_IP:-미설정}"
    printf "   %-12s %s\n" "Issuer:"   "${ISSUER_IP:-미설정}"
    printf "   %-12s %s\n" "Verifier:" "${VERIFIER_IP:-미설정}"
    printf "   %-12s %s\n" "Wallet:"   "${WALLET_IP:-미설정}"
    printf "   %-12s %s\n" "OP:"       "${OP_IP:-미설정}"
    printf "   %-12s %s\n" "API:"      "${API_IP:-미설정}"
    echo ""
fi

# -------------------------------------------------------------
# prompt_ip <label> <current_value>
#   현재값이 있으면 Enter로 유지, 없으면 필수 입력
# -------------------------------------------------------------
prompt_ip() {
    local label="$1"
    local current="$2"
    local result
    if [ -n "$current" ]; then
        read -p "   $label [$current]: " result </dev/tty
        echo "${result:-$current}"
    else
        while true; do
            read -p "   $label: " result </dev/tty
            [ -n "$result" ] && { echo "$result"; return; }
            echo "   ERROR: IP를 입력해주세요." >&2
        done
    fi
}

# -------------------------------------------------------------
# update_url <yml_file> <top_level_key> <new_url>
#   지정한 최상위 키 섹션 내의 url: 값을 교체
# -------------------------------------------------------------
update_url() {
    local yml_file="$1"
    local key="$2"
    local new_url="$3"
    grep -q "^${key}:" "$yml_file" || return 0
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "/^${key}:/,/^[^ ]/s|^  url:.*|  url: ${new_url}|" "$yml_file"
    else
        sed -i "/^${key}:/,/^[^ ]/s|^  url:.*|  url: ${new_url}|" "$yml_file"
    fi
}

# -------------------------------------------------------------
# update_springdoc <yml_file> <new_url>
#   springdoc.server.url 값을 교체
# -------------------------------------------------------------
update_springdoc() {
    local yml_file="$1"
    local new_url="$2"
    grep -q "^springdoc:" "$yml_file" || return 0
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "/^springdoc:/,/^[^ ]/s|^    url:.*|    url: ${new_url}|" "$yml_file"
    else
        sed -i "/^springdoc:/,/^[^ ]/s|^    url:.*|    url: ${new_url}|" "$yml_file"
    fi
}

# --- Step 1: IP 입력 ---
echo ">> [1/2] 각 서버가 실행될 장비의 IP를 입력하세요:"
echo "         (현재값이 있으면 Enter로 유지)"
echo ""
TAS_IP=$(prompt_ip      "TAS      (포트 4040)" "$TAS_IP")
LSS_IP=$(prompt_ip      "LSS      (포트 4141)" "$LSS_IP")
CAS_IP=$(prompt_ip      "CAS      (포트 5151)" "$CAS_IP")
ISSUER_IP=$(prompt_ip   "Issuer   (포트 4242)" "$ISSUER_IP")
VERIFIER_IP=$(prompt_ip "Verifier (포트 6060)" "$VERIFIER_IP")
WALLET_IP=$(prompt_ip   "Wallet   (포트 5252)" "$WALLET_IP")
OP_IP=$(prompt_ip       "OP       (포트 7272)" "$OP_IP")
API_IP=$(prompt_ip      "API      (포트 6363)" "$API_IP")
echo ""

# Save config
cat > "$URL_CONFIG_FILE" <<EOF
TAS_IP=$TAS_IP
LSS_IP=$LSS_IP
CAS_IP=$CAS_IP
ISSUER_IP=$ISSUER_IP
VERIFIER_IP=$VERIFIER_IP
WALLET_IP=$WALLET_IP
OP_IP=$OP_IP
API_IP=$API_IP
EOF
echo "   [OK] 설정 저장됨: .server-urls-config"
echo ""

# Derive full URLs
TAS_URL="http://${TAS_IP}:4040"
LSS_URL="http://${LSS_IP}:4141"
CAS_URL="http://${CAS_IP}:5151"
ISSUER_URL="http://${ISSUER_IP}:4242"
VERIFIER_URL="http://${VERIFIER_IP}:6060"
WALLET_URL="http://${WALLET_IP}:5252"
OP_URL="http://${OP_IP}:7272"
API_URL="http://${API_IP}:6363"

# --- Step 2: yml 업데이트 ---
echo ">> [2/2] application.yml 업데이트 중..."
echo ""

# tas: lss.url, springdoc
yml="$CONFIGS_DIR/tas/application.yml"
[ -f "$yml" ] && {
    update_url      "$yml" "lss" "$LSS_URL"
    update_springdoc "$yml" "$TAS_URL"
    echo "   [OK] tas"
}

# lss: tas.url, springdoc
yml="$CONFIGS_DIR/lss/application.yml"
[ -f "$yml" ] && {
    update_url      "$yml" "tas" "$TAS_URL"
    update_springdoc "$yml" "$LSS_URL"
    echo "   [OK] lss"
}

# cas: tas.url, lss.url, springdoc
yml="$CONFIGS_DIR/cas/application.yml"
[ -f "$yml" ] && {
    update_url      "$yml" "tas" "$TAS_URL"
    update_url      "$yml" "lss" "$LSS_URL"
    update_springdoc "$yml" "$CAS_URL"
    echo "   [OK] cas"
}

# issuer: tas.url, lss.url, springdoc
yml="$CONFIGS_DIR/issuer/application.yml"
[ -f "$yml" ] && {
    update_url      "$yml" "tas" "$TAS_URL"
    update_url      "$yml" "lss" "$LSS_URL"
    update_springdoc "$yml" "$ISSUER_URL"
    echo "   [OK] issuer"
}

# verifier: tas.url, lss.url, springdoc
yml="$CONFIGS_DIR/verifier/application.yml"
[ -f "$yml" ] && {
    update_url      "$yml" "tas" "$TAS_URL"
    update_url      "$yml" "lss" "$LSS_URL"
    update_springdoc "$yml" "$VERIFIER_URL"
    echo "   [OK] verifier"
}

# wallet: lss.url, springdoc
yml="$CONFIGS_DIR/wallet/application.yml"
[ -f "$yml" ] && {
    update_url      "$yml" "lss" "$LSS_URL"
    update_springdoc "$yml" "$WALLET_URL"
    echo "   [OK] wallet"
}

# op: tas.url, lss.url, springdoc
yml="$CONFIGS_DIR/op/application.yml"
[ -f "$yml" ] && {
    update_url      "$yml" "tas" "$TAS_URL"
    update_url      "$yml" "lss" "$LSS_URL"
    update_springdoc "$yml" "$OP_URL"
    echo "   [OK] op"
}

# api: tas.url, lss.url, springdoc
yml="$CONFIGS_DIR/api/application.yml"
[ -f "$yml" ] && {
    update_url      "$yml" "tas" "$TAS_URL"
    update_url      "$yml" "lss" "$LSS_URL"
    update_springdoc "$yml" "$API_URL"
    echo "   [OK] api"
}

# proxy: issuer.url
yml="$CONFIGS_DIR/proxy/application.yml"
[ -f "$yml" ] && {
    update_url "$yml" "issuer" "$ISSUER_URL"
    echo "   [OK] proxy"
}

echo ""
echo "============================================================"
echo ">> URL 설정 완료!"
echo "============================================================"
echo ""
echo "적용된 URL:"
printf "   %-12s %s\n" "TAS:"      "$TAS_URL"
printf "   %-12s %s\n" "LSS:"      "$LSS_URL"
printf "   %-12s %s\n" "CAS:"      "$CAS_URL"
printf "   %-12s %s\n" "Issuer:"   "$ISSUER_URL"
printf "   %-12s %s\n" "Verifier:" "$VERIFIER_URL"
printf "   %-12s %s\n" "Wallet:"   "$WALLET_URL"
printf "   %-12s %s\n" "OP:"       "$OP_URL"
printf "   %-12s %s\n" "API:"      "$API_URL"
echo ""
