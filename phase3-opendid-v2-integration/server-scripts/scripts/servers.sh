#!/bin/bash
set -u

# =============================================================
# servers.sh - OpenDID PoC Multi-Server Management Script
# =============================================================
# Usage:
#   ./servers.sh start   <server1> [server2] [server3] ...
#   ./servers.sh stop    <server1> [server2] [server3] ...
#   ./servers.sh restart <server1> [server2] [server3] ...
#   ./servers.sh status
#   ./servers.sh start all  (전체 서버를 의존성 순서로 기동)
#   ./servers.sh stop  all  (전체 서버 중지)
#
# Valid server names:
#   lss  tas(=ta)  issuer  verifier  cas  wallet  api  demo  observer
#
# Alias:
#   ta → tas
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
JARS_DIR="$PROJECT_ROOT/jars"
CONFIGS_DIR="$PROJECT_ROOT/configs"
LOGS_DIR="$PROJECT_ROOT/logs"

# Load common functions
source "$SCRIPT_DIR/server/server-lib.sh"


# --- Usage ---
usage() {
    echo ""
    echo "  Usage: $(basename "$0") <action> <server1> [server2] ..."
    echo ""
    echo "  Actions:"
    echo "    start   <server...>   하나 이상의 서버 시작 (DB 생성 여부 선택 가능)"
    echo "    stop    <server...>   하나 이상의 서버 중지"
    echo "    restart <server...>   하나 이상의 서버 재시작"
    echo "    status                전체 서버 상태 확인"
    echo ""
    echo "  'all' 키워드:"
    echo "    start all             전체 서버를 의존성 순서로 기동 (start-servers.sh 위임)"
    echo "    stop  all             전체 서버 중지"
    echo ""
    echo "  Valid server names:"
    echo "    lss      - Ledger Service Server"
    echo "    tas      - Trust Anchor Server  (alias: ta)"
    echo "    issuer   - VC Issuer Server"
    echo "    verifier - VP Verifier Server"
    echo "    cas      - CA Server"
    echo "    wallet   - Wallet Server"
    echo "    api      - API Gateway"
    echo "    demo     - Demo Server"
    echo "    observer - Observer Server"
    echo ""
    echo "  Examples:"
    echo "    $(basename "$0") start ta issuer verifier"
    echo "    $(basename "$0") stop  tas issuer"
    echo "    $(basename "$0") restart tas verifier"
    echo "    $(basename "$0") start all"
    echo "    $(basename "$0") status"
    echo ""
}

# --- Alias 처리: ta → tas ---
resolve_alias() {
    local name="$1"
    if [ "$name" == "ta" ]; then
        echo "tas"
    else
        echo "$name"
    fi
}

# --- Action: start all ---
_start_all() {
    "$SCRIPT_DIR/server/start-servers.sh"
}

# --- Action: stop all ---
_stop_all() {
    "$SCRIPT_DIR/server/stop-servers.sh"
}

# --- Action: status ---
_show_status() {
    echo "============================================================"
    echo ">> OpenDID PoC Server Status"
    echo "============================================================"
    for server in "${VALID_SERVERS[@]}"; do
        status_server "$server"
    done
    echo "============================================================"
}

# --- DB 생성 여부 확인 및 실행 ---
# 인자: 서버 목록 배열 (이미 alias 처리 완료된 이름)
_maybe_create_dbs() {
    local servers=("$@")

    # DB가 필요한 서버 목록 수집
    local db_targets=()
    for server in "${servers[@]}"; do
        local db
        db=$(get_server_db "$server")
        if [ -n "$db" ]; then
            db_targets+=("$db")
        fi
    done

    if [ ${#db_targets[@]} -eq 0 ]; then
        # 선택한 서버들은 DB가 없음
        return
    fi

    echo ""
    echo "  선택한 서버에 매핑되는 DB: [${db_targets[*]}]"
    echo ""
    printf "  >> DB를 생성하시겠습니까? (y/N): "
    read -r answer </dev/tty

    case "$answer" in
        [Yy]|[Yy][Ee][Ss])
            echo ""
            echo ">> DB 생성 시작: [${db_targets[*]}]"
            "$SCRIPT_DIR/postgres/20-init-databases.sh" "${db_targets[@]}"
            echo ""
            ;;
        *)
            echo "   DB 생성을 건너뜁니다."
            echo ""
            ;;
    esac
}

# --- Argument Parsing ---
ACTION="${1:-}"

if [ -z "$ACTION" ]; then
    echo "ERROR: 액션을 지정해 주세요."
    usage
    exit 1
fi

shift  # ACTION 제거 후 나머지는 서버 목록

# --- Main Dispatch ---
case "$ACTION" in

    start)
        TARGETS=("$@")
        if [ ${#TARGETS[@]} -eq 0 ]; then
            echo "ERROR: 시작할 서버를 하나 이상 지정해 주세요."
            usage
            exit 1
        fi

        # 'all' 단독 처리
        if [ ${#TARGETS[@]} -eq 1 ] && [ "${TARGETS[0]}" == "all" ]; then
            DB_CONFIG_FILE="$PROJECT_ROOT/.db-config"

            # Check if database is configured
            if [ ! -f "$DB_CONFIG_FILE" ]; then
                echo ""
                echo "ERROR: Database is not configured."
                echo ""
                echo "Please run one of the following setup scripts first:"
                echo "  ./setup-local-db.sh      # Install PostgreSQL locally"
                echo "  ./setup-external-db.sh   # Configure external database"
                echo ""
                exit 1
            fi

            # Load database configuration and check PostgreSQL if needed
            source "$DB_CONFIG_FILE"
            if [ "${INSTALL_DB:-}" = "yes" ]; then
                ensure_postgres_running
                echo ""
            fi

            _start_all
            exit 0
        fi

        # Alias 처리 및 유효성 검사
        RESOLVED=()
        for target in "${TARGETS[@]}"; do
            target=$(resolve_alias "$target")
            validate_server_name "$target"
            RESOLVED+=("$target")
        done

        # DB 필요 서버가 하나라도 있으면 DB 설정 확인
        has_db_server=false
        for _t in "${RESOLVED[@]}"; do
            if [ -n "$(get_server_db "$_t")" ]; then
                has_db_server=true
                break
            fi
        done

        if [ "$has_db_server" = true ]; then
            DB_CONFIG_FILE="$PROJECT_ROOT/.db-config"

            # Check if database is configured
            if [ ! -f "$DB_CONFIG_FILE" ]; then
                echo ""
                echo "ERROR: Database is not configured."
                echo ""
                echo "Please run one of the following setup scripts first:"
                echo "  ./setup-local-db.sh      # Install PostgreSQL locally"
                echo "  ./setup-external-db.sh   # Configure external database"
                echo ""
                exit 1
            fi

            # Load database configuration
            source "$DB_CONFIG_FILE"

            if [ "${INSTALL_DB:-}" = "yes" ]; then
                ensure_postgres_running
                echo ""
                # DB 생성 여부 확인
                _maybe_create_dbs "${RESOLVED[@]}"
            fi
        fi

        echo "============================================================"
        echo ">> Starting servers: [${RESOLVED[*]}]"
        echo "============================================================"
        for target in "${RESOLVED[@]}"; do
            start_server "$target"
        done
        echo "============================================================"
        echo ">> 지정한 서버 기동 완료."
        echo "============================================================"
        ;;

    stop)
        TARGETS=("$@")
        if [ ${#TARGETS[@]} -eq 0 ]; then
            echo "ERROR: 중지할 서버를 하나 이상 지정해 주세요."
            usage
            exit 1
        fi

        # 'all' 단독 처리
        if [ ${#TARGETS[@]} -eq 1 ] && [ "${TARGETS[0]}" == "all" ]; then
            _stop_all
            exit 0
        fi

        echo "============================================================"
        echo ">> Stopping servers: [${TARGETS[*]}]"
        echo "============================================================"
        for target in "${TARGETS[@]}"; do
            target=$(resolve_alias "$target")
            validate_server_name "$target"
            stop_server "$target"
        done
        echo "============================================================"
        echo ">> 지정한 서버 중지 완료."
        echo "============================================================"
        ;;

    restart)
        TARGETS=("$@")
        if [ ${#TARGETS[@]} -eq 0 ]; then
            echo "ERROR: 재시작할 서버를 하나 이상 지정해 주세요."
            usage
            exit 1
        fi

        if [ ${#TARGETS[@]} -eq 1 ] && [ "${TARGETS[0]}" == "all" ]; then
            echo "ERROR: 'restart all' 은 지원하지 않습니다."
            echo "  대신 사용: ./stop-all.sh && ./start-all.sh"
            exit 1
        fi

        echo "============================================================"
        echo ">> Restarting servers: [${TARGETS[*]}]"
        echo "============================================================"
        for target in "${TARGETS[@]}"; do
            target=$(resolve_alias "$target")
            validate_server_name "$target"
            echo "-- Restarting [$target] --"
            stop_server "$target"
            echo "   Waiting before restart (3s)..."
            sleep 3
            start_server "$target"
        done
        echo "============================================================"
        echo ">> 지정한 서버 재시작 완료."
        echo "============================================================"
        ;;

    status)
        _show_status
        ;;

    help|--help|-h)
        usage
        ;;

    *)
        echo "ERROR: 알 수 없는 액션: '$ACTION'"
        usage
        exit 1
        ;;

esac
