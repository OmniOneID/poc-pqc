#!/bin/bash
set -euo pipefail

# OpenDID PoC Environment - External Database Setup
# This script configures the connection to an external PostgreSQL database.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DB_CONFIG_FILE="$PROJECT_ROOT/.db-config"

echo "============================================================"
echo ">> External Database Setup"
echo "============================================================"
echo ""
echo "This will configure the connection to an external PostgreSQL database."
echo ""
echo "Prerequisites:"
echo "  - PostgreSQL server is already running"
echo "  - 7 databases must exist: tas, cas, issuer, verifier, wallet, op, lss"
echo "  - User has permissions to access these databases"
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

# Step 1: Get database connection info
echo ">> [Step 1/2] Database Connection Settings"
echo ""

# Load existing values if available
if [ -f "$DB_CONFIG_FILE" ]; then
    source "$DB_CONFIG_FILE"
fi

while true; do
    read -p "   Enter database host (e.g., 192.168.1.100): " INPUT_HOST
    INPUT_HOST="${INPUT_HOST:-${DB_HOST:-}}"
    if [ -n "$INPUT_HOST" ]; then
        break
    fi
    echo "   ERROR: Database host is required."
done

read -p "   Enter database port [5432]: " INPUT_PORT
INPUT_PORT="${INPUT_PORT:-5432}"

read -p "   Enter database username [omn]: " INPUT_USER
INPUT_USER="${INPUT_USER:-omn}"

while true; do
    read -p "   Enter database password: " INPUT_PASSWORD
    if [ -n "$INPUT_PASSWORD" ]; then
        break
    fi
    echo "   ERROR: Database password is required."
done

read -p "   Enter spring.profiles.active [dev]: " INPUT_PROFILE
INPUT_PROFILE="${INPUT_PROFILE:-dev}"

echo ""

# Save configuration
cat > "$DB_CONFIG_FILE" <<EOF
INSTALL_DB=no
DB_HOST=$INPUT_HOST
DB_PORT=$INPUT_PORT
DB_USERNAME=$INPUT_USER
DB_PASSWORD=$INPUT_PASSWORD
EOF

echo "   [OK] Configuration saved to .db-config"
echo "   Host: $INPUT_HOST"
echo "   Port: $INPUT_PORT"
echo "   Username: $INPUT_USER"
echo ""

# Step 2: Update application.yml files
echo ">> [Step 2/2] Updating application.yml files..."
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
        awk -v host="$INPUT_HOST" -v port="$INPUT_PORT" -v dbname="$db_name" -v user="$INPUT_USER" -v pass="$INPUT_PASSWORD" '
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
        awk -v host="$INPUT_HOST" -v port="$INPUT_PORT" -v dbname="$db_name" -v user="$INPUT_USER" -v pass="$INPUT_PASSWORD" '
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
    new_url="jdbc:postgresql://$INPUT_HOST:$INPUT_PORT/verifier"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "/^  datasource:/,/^  [a-z]/s|^    url:.*|    url: $new_url|"      "$jv_config"
        sed -i '' "/^  datasource:/,/^  [a-z]/s|^    username:.*|    username: $INPUT_USER|" "$jv_config"
        sed -i '' "/^  datasource:/,/^  [a-z]/s|^    password:.*|    password: $INPUT_PASSWORD|" "$jv_config"
    else
        sed -i "/^  datasource:/,/^  [a-z]/s|^    url:.*|    url: $new_url|"      "$jv_config"
        sed -i "/^  datasource:/,/^  [a-z]/s|^    username:.*|    username: $INPUT_USER|" "$jv_config"
        sed -i "/^  datasource:/,/^  [a-z]/s|^    password:.*|    password: $INPUT_PASSWORD|" "$jv_config"
    fi
    echo "   [UPDATE] j-verifier - datasource updated (profiles.active: jpa 유지)"
fi

echo ""
echo "============================================================"
echo ">> External Database Setup Complete!"
echo "============================================================"
echo ""
echo "Database configuration:"
echo "  - External PostgreSQL: $INPUT_HOST:$INPUT_PORT"
echo "  - Username: $INPUT_USER"
echo "  - application.yml files updated"
echo ""
echo "IMPORTANT: Make sure these databases exist on the external server:"
echo "  tas, cas, issuer, verifier, wallet, op, lss"
echo ""
echo "Next step:"
echo "  ./start-all.sh    # Start all servers"
echo "============================================================"
