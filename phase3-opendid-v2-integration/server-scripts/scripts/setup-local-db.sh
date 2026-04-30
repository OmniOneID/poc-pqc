#!/bin/bash
set -euo pipefail

# OpenDID PoC Environment - Local PostgreSQL Setup
# This script installs PostgreSQL locally and configures the databases.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DB_CONFIG_FILE="$PROJECT_ROOT/.db-config"

echo "============================================================"
echo ">> Local Database Setup"
echo "============================================================"
echo ""
echo "This will:"
echo "  1. Check PostgreSQL installation"
echo "  2. Create 7 databases for OpenDID servers"
echo "  3. Save database configuration"
echo "  4. Configure application.yml files"
echo ""

# Check if already configured
if [ -f "$DB_CONFIG_FILE" ]; then
    source "$DB_CONFIG_FILE"
    if [ -n "${INSTALL_DB:-}" ]; then
        echo "WARNING: Database configuration already exists."
        echo "Current setting: INSTALL_DB=$INSTALL_DB"
        echo ""
        read -p "Do you want to reconfigure? (yes/no) [no]: " RECONFIGURE
        RECONFIGURE="${RECONFIGURE:-no}"
        if [ "$RECONFIGURE" != "yes" ]; then
            echo "Setup cancelled. Existing configuration preserved."
            exit 0
        fi
        echo ""
    fi
fi

# Step 1: Check PostgreSQL installation
echo ">> [Step 1/3] Checking PostgreSQL installation..."

POSTGRES_CONFIG_FILE="$PROJECT_ROOT/.postgres-config"
if [ ! -f "$POSTGRES_CONFIG_FILE" ]; then
    echo ""
    echo "ERROR: PostgreSQL is not installed."
    echo ""
    echo "Please install PostgreSQL first:"
    echo "  ./install-postgres.sh"
    echo ""
    exit 1
fi

# Load PostgreSQL configuration
source "$POSTGRES_CONFIG_FILE"
CONTAINER_NAME="${CONTAINER_NAME:-opendid-poc-postgres}"

# Check if PostgreSQL container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo ""
    echo "WARNING: PostgreSQL container '$CONTAINER_NAME' is not running."

    # Check if container exists but stopped
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "Starting existing container..."
        docker start "$CONTAINER_NAME"
        sleep 3
    else
        echo ""
        echo "ERROR: PostgreSQL container does not exist."
        echo ""
        echo "Please install PostgreSQL first:"
        echo "  ./install-postgres.sh"
        echo ""
        exit 1
    fi
fi

echo "   [OK] PostgreSQL is running"
echo ""

# Step 2: Create databases
echo ">> [Step 2/3] Creating databases..."
"$SCRIPT_DIR/postgres/20-init-databases.sh"
echo ""

# Step 3: Save database configuration
echo ">> [Step 3/4] Saving database configuration..."

# Use default local PostgreSQL settings
DB_HOST="127.0.0.1"
DB_PORT="${POSTGRES_PORT:-5430}"
DB_USER="${POSTGRES_USER:-omn}"
DB_PASSWORD="${POSTGRES_PASSWORD:-omn}"

cat > "$DB_CONFIG_FILE" <<EOF
INSTALL_DB=yes
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_USERNAME=$DB_USER
DB_PASSWORD=$DB_PASSWORD
EOF

echo "   [OK] Configuration saved to .db-config"
echo "   Host: $DB_HOST"
echo "   Port: $DB_PORT"
echo "   Username: $DB_USER"
echo ""

# Step 4: Update application.yml files
echo ">> [Step 4/4] Updating application.yml files..."
echo ""
read -p "   Enter spring.profiles.active [dev]: " INPUT_PROFILE
INPUT_PROFILE="${INPUT_PROFILE:-dev}"
echo ""

CONFIGS_DIR="$PROJECT_ROOT/configs"
SERVERS=("tas" "cas" "issuer" "verifier" "wallet" "op" "lss")

for server in "${SERVERS[@]}"; do
    db_name="$server"
    config_file="$CONFIGS_DIR/$server/application.yml"

    if [ ! -f "$config_file" ]; then
        echo "   [SKIP] $server - Config file not found"
        continue
    fi

    # Backup original file
    cp "$config_file" "${config_file}.bak"

    # Check if database config already exists
    if grep -q "datasource:" "$config_file"; then
        # Update existing config
        awk -v host="$DB_HOST" -v port="$DB_PORT" -v dbname="$db_name" -v user="$DB_USER" -v pass="$DB_PASSWORD" '
        BEGIN { in_spring=0; in_db_section=0 }
        /^spring:/ {
            in_spring=1
            print
            next
        }
        in_spring && /^  [a-z]/ {
            if (/^  (liquibase|datasource|jpa):/) {
                if (!in_db_section) {
                    in_db_section=1
                    print "  liquibase:"
                    print "    change-log: classpath:/db/changelog/master.xml"
                    print "    enabled: true"
                    print "  datasource:"
                    print "    driver-class-name: org.postgresql.Driver"
                    print "    url: jdbc:postgresql://" host ":" port "/" dbname
                    print "    username: " user
                    print "    password: " pass
                    print "  jpa:"
                    print "    open-in-view: true"
                    print "    show-sql: false"
                    print "    hibernate:"
                    print "      ddl-auto: none"
                    print "      naming:"
                    print "        physical-strategy: org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
                    print "    properties:"
                    print "      hibernate:"
                    print "        format_sql: false"
                }
                next
            } else if (/^  profiles:/) {
                print
                next
            } else {
                in_db_section=0
                in_spring=0
                print
                next
            }
        }
        in_db_section && /^[a-z]/ {
            in_db_section=0
            in_spring=0
            print
            next
        }
        in_db_section && /^  [a-z]/ {
            in_db_section=0
            in_spring=0
            print
            next
        }
        in_db_section {
            next
        }
        !in_db_section {
            print
        }
        ' "${config_file}.bak" > "$config_file"
        echo "   [UPDATE] $server"
    else
        # Add new config
        awk -v host="$DB_HOST" -v port="$DB_PORT" -v dbname="$db_name" -v user="$DB_USER" -v pass="$DB_PASSWORD" '
        /^  profiles:/ {
            in_profiles=1
            print
            next
        }
        in_profiles && /^    active:/ {
            print
            print ""
            print "  liquibase:"
            print "    change-log: classpath:/db/changelog/master.xml"
            print "    enabled: true"
            print "  datasource:"
            print "    driver-class-name: org.postgresql.Driver"
            print "    url: jdbc:postgresql://" host ":" port "/" dbname
            print "    username: " user
            print "    password: " pass
            print "  jpa:"
            print "    open-in-view: true"
            print "    show-sql: false"
            print "    hibernate:"
            print "      ddl-auto: none"
            print "      naming:"
            print "        physical-strategy: org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
            print "    properties:"
            print "      hibernate:"
            print "        format_sql: false"
            in_profiles=0
            next
        }
        { print }
        ' "${config_file}.bak" > "$config_file"
        echo "   [ADD] $server"
    fi

    # Update spring.profiles.active
    if grep -q "^    active:" "$config_file"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s/^    active:.*$/    active: $INPUT_PROFILE/" "$config_file"
        else
            sed -i "s/^    active:.*$/    active: $INPUT_PROFILE/" "$config_file"
        fi
        echo "   [PROFILE] $server -> spring.profiles.active: $INPUT_PROFILE"
    fi

    # Update blockchain.file-path if blockchain.properties exists
    blockchain_props="$CONFIGS_DIR/$server/blockchain.properties"
    if [ -f "$blockchain_props" ]; then
        if grep -q "^blockchain:" "$config_file"; then
            if [[ "$OSTYPE" == "darwin"* ]]; then
                sed -i '' "/^blockchain:/,/^[^ ]/s|^  file-path:.*$|  file-path: $blockchain_props|" "$config_file"
            else
                sed -i "/^blockchain:/,/^[^ ]/s|^  file-path:.*$|  file-path: $blockchain_props|" "$config_file"
            fi
        else
            printf '\nblockchain:\n  file-path: %s\n' "$blockchain_props" >> "$config_file"
        fi
        echo "   [BLOCKCHAIN] $server -> blockchain.file-path: $blockchain_props"
    fi
done

api_config="$CONFIGS_DIR/api/application.yml"
if [ -f "$api_config" ] && grep -q "^    active:" "$api_config"; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^    active:.*$/    active: $INPUT_PROFILE/" "$api_config"
    else
        sed -i "s/^    active:.*$/    active: $INPUT_PROFILE/" "$api_config"
    fi
    echo "   [PROFILE] api -> spring.profiles.active: $INPUT_PROFILE"
fi

api_blockchain="$CONFIGS_DIR/api/blockchain.properties"
if [ -f "$api_config" ] && [ -f "$api_blockchain" ]; then
    if grep -q "^blockchain:" "$api_config"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "/^blockchain:/,/^[^ ]/s|^  file-path:.*$|  file-path: $api_blockchain|" "$api_config"
        else
            sed -i "/^blockchain:/,/^[^ ]/s|^  file-path:.*$|  file-path: $api_blockchain|" "$api_config"
        fi
    else
        printf '\nblockchain:\n  file-path: %s\n' "$api_blockchain" >> "$api_config"
    fi
    echo "   [BLOCKCHAIN] api -> blockchain.file-path: $api_blockchain"
fi

# j-verifier: datasource만 sed로 업데이트 (profiles.active는 'jpa' 유지)
jv_config="$CONFIGS_DIR/j-verifier/application.yml"
if [ -f "$jv_config" ]; then
    cp "$jv_config" "${jv_config}.bak"
    new_url="jdbc:postgresql://$DB_HOST:$DB_PORT/verifier"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "/^  datasource:/,/^  [a-z]/s|^    url:.*|    url: $new_url|"      "$jv_config"
        sed -i '' "/^  datasource:/,/^  [a-z]/s|^    username:.*|    username: $DB_USER|" "$jv_config"
        sed -i '' "/^  datasource:/,/^  [a-z]/s|^    password:.*|    password: $DB_PASSWORD|" "$jv_config"
    else
        sed -i "/^  datasource:/,/^  [a-z]/s|^    url:.*|    url: $new_url|"      "$jv_config"
        sed -i "/^  datasource:/,/^  [a-z]/s|^    username:.*|    username: $DB_USER|" "$jv_config"
        sed -i "/^  datasource:/,/^  [a-z]/s|^    password:.*|    password: $DB_PASSWORD|" "$jv_config"
    fi
    echo "   [UPDATE] j-verifier - datasource updated (profiles.active: jpa 유지)"
fi

echo ""
echo "============================================================"
echo ">> Local Database Setup Complete!"
echo "============================================================"
echo ""
echo "Configuration:"
echo "  - 7 databases created (tas, cas, issuer, verifier, wallet, op, lss)"
echo "  - application.yml files updated"
echo "  - Local PostgreSQL (127.0.0.1:${DB_PORT})"
echo ""
echo "Next step:"
echo "  ./start-all.sh    # Start all servers"
echo "============================================================"
