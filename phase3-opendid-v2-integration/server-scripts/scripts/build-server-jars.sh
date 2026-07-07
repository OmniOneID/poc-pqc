#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

WITH_FRONTEND=false
CLEAN=false

usage() {
    cat <<'EOF'
Usage: ./build-server-jars.sh [options]

Build all OpenDID server jars and copy them into server-scripts/jars/* via each project's Gradle build.

Options:
  --with-frontend  Build frontend assets for CA, Issuer, TA, Verifier, Wallet as well.
  --clean          Run clean bootJar instead of bootJar.
  -h, --help       Show this help.
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --with-frontend)
            WITH_FRONTEND=true
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

GRADLE_TASK="bootJar"
if [[ "${CLEAN}" == "true" ]]; then
    GRADLE_TASK="clean bootJar"
fi

SERVERS=(
    "API|did-api-server/source/did-api-server|false"
    "CA|did-ca-server/source/did-ca-server|true"
    "Demo|did-demo-server/source/did-demo-server|false"
    "Issuer|did-issuer-server/source/did-issuer-server|true"
    "LSS|did-ledger-service-server/source/did-ledger-service-server|false"
    "Observer|did-observer-server/source/did-observer-server|false"
    "TA|did-ta-server/source/did-ta-server|true"
    "Verifier|did-verifier-server/source/did-verifier-server|true"
    "Wallet|did-wallet-server/source/did-wallet-server|true"
)

echo "Root directory: ${ROOT_DIR}"
echo "Frontend build: ${WITH_FRONTEND}"
echo "Gradle task: ${GRADLE_TASK}"
echo

for entry in "${SERVERS[@]}"; do
    IFS='|' read -r jar_dir project_rel needs_frontend <<< "${entry}"
    project_dir="${ROOT_DIR}/${project_rel}"

    if [[ ! -d "${project_dir}" ]]; then
        echo "[ERROR] Missing project directory: ${project_dir}" >&2
        exit 1
    fi

    gradle_cmd=(bash ./gradlew)
    if [[ "${needs_frontend}" == "true" && "${WITH_FRONTEND}" == "false" ]]; then
        gradle_cmd+=("-DskipFrontendBuild=true")
    fi

    if [[ "${CLEAN}" == "true" ]]; then
        gradle_cmd+=(clean bootJar)
    else
        gradle_cmd+=(bootJar)
    fi

    echo "==> Building ${jar_dir}"
    (
        cd "${project_dir}"
        "${gradle_cmd[@]}"
    )

    jar_output_dir="${ROOT_DIR}/server-scripts/jars/${jar_dir}"
    if compgen -G "${jar_output_dir}/*.jar" > /dev/null; then
        echo "    Copied jar to ${jar_output_dir}"
    else
        echo "[ERROR] No jar found in ${jar_output_dir} after build" >&2
        exit 1
    fi
    echo
done

echo "All server jars were built successfully."
