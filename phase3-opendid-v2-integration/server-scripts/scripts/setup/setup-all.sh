#!/bin/bash
set -euo pipefail

# =============================================================
# setup-all.sh - OpenDID PoC Full Server Setup Automation
# =============================================================
# Runs all server setup scripts in the correct order:
#   1. TA (sequential, must complete first)
#   2. Wallet / CA / Issuer / Verifier (parallel)
#   3. Admin Policy (sequential: TA KYC → Issuer VC policy → Verifier VP policy)
#
# Prerequisites:
#   - Setup Reset must have been performed beforehand
#   - All servers must be running
#
# Usage:
#   ./setup-all.sh [options]
#
# Options:
#   --ip <ip>        Server IP address
#   --work-dir <dir> Base artifact output directory (default: logs/setup-all)
#   -h, --help       Show this help
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

_LOCAL_IP=""
WORK_DIR="$PROJECT_ROOT/logs/setup-all"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ip)        _LOCAL_IP="$2"; shift 2 ;;
        --work-dir)  WORK_DIR="$2";  shift 2 ;;
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

_IP_CACHE="$PROJECT_ROOT/.last-ip"
_SAVED_IP=""
if [ -f "$_IP_CACHE" ]; then
    _SAVED_IP=$(cat "$_IP_CACHE")
fi

if [ -z "$_LOCAL_IP" ]; then
    if [ -n "$_SAVED_IP" ]; then
        read -rp "Server IP [$_SAVED_IP]: " _LOCAL_IP
        if [ -z "$_LOCAL_IP" ]; then
            _LOCAL_IP="$_SAVED_IP"
        fi
    else
        read -rp "Server IP: " _LOCAL_IP
    fi
fi

echo "$_LOCAL_IP" > "$_IP_CACHE"

mkdir -p "$WORK_DIR"

LOG_DIR="$WORK_DIR/logs"
mkdir -p "$LOG_DIR"

# =============================================================
# Step 1: TA 셋업 (순차 실행, 완료 후 다음 단계)
# =============================================================

echo ""
echo "============================================================="
echo "  [1/3] TA Setup"
echo "============================================================="

"$SCRIPT_DIR/servers/setup-ta.sh" \
    --ip "$_LOCAL_IP" \
    --work-dir "$WORK_DIR/ta" \
    --initial-password "password" \
    --new-password "password" \
    2>&1 | tee "$LOG_DIR/ta.log"

if [ "${PIPESTATUS[0]}" -ne 0 ]; then
    echo ""
    echo "ERROR: TA setup failed. See $LOG_DIR/ta.log"
    exit 1
fi

echo ""
echo "  TA setup complete. Starting parallel setup..."
echo ""

# =============================================================
# Step 2: Wallet / CA / Issuer / Verifier 병렬 실행
# =============================================================

echo "============================================================="
echo "  [2/3] Wallet / CA / Issuer / Verifier Setup (parallel)"
echo "============================================================="
echo ""

# Step 11 (enroll entity) 직렬화용 공유 뮤텍스 디렉터리
# TA의 wallet signing이 동시 접근을 허용하지 않아 병렬 실행 시 SSRVTRA16512 발생
ENROLL_LOCK="$WORK_DIR/enroll.lock"

"$SCRIPT_DIR/servers/setup-wallet.sh" \
    --ip "$_LOCAL_IP" \
    --work-dir "$WORK_DIR/wallet" \
    --lock-dir "$ENROLL_LOCK" \
    > "$LOG_DIR/wallet.log" 2>&1 &
PID_WALLET=$!

"$SCRIPT_DIR/servers/setup-ca.sh" \
    --ip "$_LOCAL_IP" \
    --work-dir "$WORK_DIR/ca" \
    --lock-dir "$ENROLL_LOCK" \
    > "$LOG_DIR/ca.log" 2>&1 &
PID_CA=$!

"$SCRIPT_DIR/servers/setup-issuer.sh" \
    --ip "$_LOCAL_IP" \
    --work-dir "$WORK_DIR/issuer" \
    --lock-dir "$ENROLL_LOCK" \
    > "$LOG_DIR/issuer.log" 2>&1 &
PID_ISSUER=$!

"$SCRIPT_DIR/servers/setup-verifier.sh" \
    --ip "$_LOCAL_IP" \
    --work-dir "$WORK_DIR/verifier" \
    --lock-dir "$ENROLL_LOCK" \
    > "$LOG_DIR/verifier.log" 2>&1 &
PID_VERIFIER=$!

echo "  wallet   (PID $PID_WALLET)"
echo "  ca       (PID $PID_CA)"
echo "  issuer   (PID $PID_ISSUER)"
echo "  verifier (PID $PID_VERIFIER)"
echo ""
echo "  Waiting for all to complete..."
echo ""

FAILED=0

wait_check() {
    local name="$1"
    local pid="$2"
    local log="$3"
    if wait "$pid"; then
        echo "  [OK]  $name"
    else
        echo "  [FAIL] $name — see $log"
        FAILED=1
    fi
}

wait_check "wallet"   "$PID_WALLET"   "$LOG_DIR/wallet.log"
wait_check "ca"       "$PID_CA"       "$LOG_DIR/ca.log"
wait_check "issuer"   "$PID_ISSUER"   "$LOG_DIR/issuer.log"
wait_check "verifier" "$PID_VERIFIER" "$LOG_DIR/verifier.log"

echo ""

if [ "$FAILED" -ne 0 ]; then
    echo "============================================================="
    echo "  One or more entity servers failed. Check logs in $LOG_DIR"
    echo "  Skipping admin policy setup."
    echo "============================================================="
    exit 1
fi

echo "  All entity servers setup complete."
echo ""

# =============================================================
# Step 3: Admin Policy 설정 (순차 실행)
# =============================================================

echo "============================================================="
echo "  [3/3] Admin Policy Setup"
echo "============================================================="

"$SCRIPT_DIR/servers/setup-admin-policy.sh" \
    --ip "$_LOCAL_IP" \
    --work-dir "$WORK_DIR/admin-policy" \
    2>&1 | tee "$LOG_DIR/admin-policy.log"

if [ "${PIPESTATUS[0]}" -ne 0 ]; then
    echo ""
    echo "ERROR: Admin policy setup failed. See $LOG_DIR/admin-policy.log"
    exit 1
fi

echo ""
echo "============================================================="
echo "  All setup complete."
echo "  Logs: $LOG_DIR"
echo "============================================================="
