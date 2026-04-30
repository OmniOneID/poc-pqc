#!/bin/bash
# =============================================================
# server-lib.sh - OpenDID PoC Common Server Management Library
# =============================================================
# This file is a LIBRARY — source it, do NOT execute directly.
# Requires the sourcing script to set these variables BEFORE sourcing:
#   - JARS_DIR   : absolute path to the jars directory
#   - CONFIGS_DIR: absolute path to the configs directory
#   - LOGS_DIR   : absolute path to the logs directory
# =============================================================

# Guard: prevent direct execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "ERROR: server-lib.sh is a library. Do not run it directly."
    echo "  Usage: source $(basename "${BASH_SOURCE[0]}")"
    exit 1
fi

# List of all valid server names
VALID_SERVERS=("lss" "tas" "issuer" "verifier" "cas" "wallet" "api" "demo" "observer")

# -------------------------------------------------------------
# get_server_db <server_name>
#   Prints the DB name required by the server, or empty string if none.
# -------------------------------------------------------------
get_server_db() {
    local server="$1"
    case "$server" in
        "tas")        echo "tas" ;;
        "issuer")     echo "issuer" ;;
        "verifier")   echo "verifier" ;;
        "cas")        echo "cas" ;;
        "wallet")     echo "wallet" ;;
        "lss")        echo "lss" ;;
        *)            echo "" ;;
    esac
}

# -------------------------------------------------------------
# ensure_postgres_running
#   Checks that the PostgreSQL container is running.
#   - Reads .postgres-config for container name (falls back to default).
#   - If container is stopped, starts it automatically.
#   - If container does not exist, exits with error.
# -------------------------------------------------------------
ensure_postgres_running() {
    local _lib_script_dir
    _lib_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    local _project_root
    _project_root="$(cd "$_lib_script_dir/../.." && pwd)"
    local _config_file="$_project_root/.postgres-config"

    local _container_name="opendid-poc-postgres"
    if [ -f "$_config_file" ]; then
        local _stored
        _stored=$(grep "^CONTAINER_NAME=" "$_config_file" | cut -d'=' -f2)
        [ -n "$_stored" ] && _container_name="$_stored"
    fi

    echo ">> [PostgreSQL] Checking container: $_container_name"

    if ! command -v docker &>/dev/null; then
        echo "ERROR: docker command not found. Please install Docker first."
        exit 1
    fi

    if docker ps --format '{{.Names}}' | grep -q "^${_container_name}$"; then
        echo "   [OK] PostgreSQL container is running."
        return 0
    fi

    if docker ps -a --format '{{.Names}}' | grep -q "^${_container_name}$"; then
        echo "   INFO: Container exists but is stopped. Starting it..."
        docker start "$_container_name"

        local _timeout=30
        local _elapsed=0
        until [ $_elapsed -ge $_timeout ]; do
            if docker exec "$_container_name" pg_isready -U omn > /dev/null 2>&1; then
                echo "   [OK] PostgreSQL is ready."
                return 0
            fi
            sleep 2
            _elapsed=$((_elapsed + 2))
        done

        echo "ERROR: Timeout waiting for PostgreSQL to be ready."
        exit 1
    fi

    echo "ERROR: PostgreSQL container '$_container_name' does not exist."
    echo "  Please run the following first:"
    echo "    scripts/postgres/run-all.sh"
    exit 1
}

# -------------------------------------------------------------
# get_jar_path <server_name>
#   Prints the absolute path of the JAR file for the given server.
#   Prints nothing if the server is unknown or JAR is not found.
# -------------------------------------------------------------
get_jar_path() {
    local server_name="$1"
    local folder_name=""

    case "$server_name" in
        "tas")    folder_name="TA" ;;
        "cas")    folder_name="CA" ;;
        "issuer") folder_name="Issuer" ;;
        "verifier") folder_name="Verifier" ;;
        "wallet") folder_name="Wallet" ;;
        "lss")    folder_name="LSS" ;;
        "api")      folder_name="API" ;;
        "demo")     folder_name="Demo" ;;
        "observer") folder_name="Observer" ;;
        *) return ;;
    esac

    local jar_path
    jar_path=$(find "$JARS_DIR/$folder_name" -maxdepth 1 -name "*.jar" 2>/dev/null | head -n 1)
    echo "$jar_path"
}

# -------------------------------------------------------------
# validate_server_name <server_name>
#   Exits with an error if the given name is not a valid server.
# -------------------------------------------------------------
validate_server_name() {
    local server_name="$1"
    for s in "${VALID_SERVERS[@]}"; do
        if [ "$s" == "$server_name" ]; then
            return 0
        fi
    done
    echo "ERROR: Unknown server name: '$server_name'"
    echo "  Valid names: ${VALID_SERVERS[*]}"
    exit 1
}

# -------------------------------------------------------------
# start_server <server_name> [restart=false]
#   Starts the specified server as a background process.
#   If restart=true, kills the existing process first.
# -------------------------------------------------------------
start_server() {
    local server_name="$1"
    local restart="${2:-false}"

    echo ">> Starting [$server_name]..."

    # Capture separately to avoid masking errors with 'local var=$(...)'
    local jar_path
    jar_path=$(get_jar_path "$server_name")

    if [ -z "$jar_path" ]; then
        echo "   ERROR: JAR not found for '$server_name'. Check $JARS_DIR."
        return 1
    fi

    local config_file="$CONFIGS_DIR/$server_name/application.yml"
    if [ ! -f "$config_file" ]; then
        echo "   WARNING: Config not found at $config_file. Starting without config override."
        config_file=""
    fi

    local log_file="$LOGS_DIR/${server_name}.log"
    mkdir -p "$LOGS_DIR"

    # Kill existing process if restart mode
    if [ "$restart" == "true" ]; then
        echo "   Stopping existing process..."
        pkill -f "$jar_path" || true
        sleep 2
    fi

    local blockchain_props="$CONFIGS_DIR/$server_name/blockchain.properties"

    # Build config locations for spring.config.additional-location (comma-separated)
    local config_locations=""
    if [ -n "$config_file" ]; then
        config_locations="file:${config_file}"
    fi
    if [ -f "$blockchain_props" ]; then
        if [ -n "$config_locations" ]; then
            config_locations="${config_locations},file:${blockchain_props}"
        else
            config_locations="file:${blockchain_props}"
        fi
    fi

    # Build command as an array (safe against paths with spaces)
    # NOTE: JVM 옵션(-D)은 반드시 -jar 앞에 위치해야 합니다.
    local java_args=("nohup" "java")
    if [ -n "$config_locations" ]; then
        java_args+=("-Dspring.config.additional-location=${config_locations}")
    fi
    java_args+=("-jar" "$jar_path")

    # Execute — redirection and backgrounding handled outside the array
    echo "   Command: ${java_args[*]}"
    "${java_args[@]}" > "$log_file" 2>&1 &

    echo "   [OK] Started. (Log: logs/${server_name}.log)"
    echo "   Waiting for startup (5s)..."
    sleep 5
}

# -------------------------------------------------------------
# stop_server <server_name>
#   Stops the specified server by matching its JAR path pattern.
# -------------------------------------------------------------
stop_server() {
    local server_name="$1"
    local folder_name=""

    case "$server_name" in
        "tas")    folder_name="TA" ;;
        "cas")    folder_name="CA" ;;
        "issuer") folder_name="Issuer" ;;
        "verifier") folder_name="Verifier" ;;
        "wallet") folder_name="Wallet" ;;
        "lss")    folder_name="LSS" ;;
        "api")      folder_name="API" ;;
        "demo")     folder_name="Demo" ;;
        "observer") folder_name="Observer" ;;
        *)
            echo "   WARNING: Unknown server '$server_name'. Skipping."
            return
            ;;
    esac

    echo "   Stopping $server_name (folder: $folder_name)..."
    pkill -fi "jars/$folder_name/.*\.jar" || echo "   (No running process found for $server_name)"
}

# -------------------------------------------------------------
# status_server <server_name>
#   Prints a single-line status (RUNNING/STOPPED/NO JAR) for the server.
# -------------------------------------------------------------
status_server() {
    local server_name="$1"

    local jar_path
    jar_path=$(get_jar_path "$server_name")

    if [ -z "$jar_path" ]; then
        printf "  %-20s  [NO JAR ]  (JAR not found in %s)\n" "$server_name" "$JARS_DIR"
        return
    fi

    if pgrep -f "$jar_path" > /dev/null 2>&1; then
        local pid
        pid=$(pgrep -f "$jar_path" | head -n 1)
        printf "  %-20s  [RUNNING]  PID: %s\n" "$server_name" "$pid"
    else
        printf "  %-20s  [STOPPED]\n" "$server_name"
    fi
}
