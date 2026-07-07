#!/bin/bash
set -euo pipefail

# OpenDID PoC Environment - Database Configuration Setup
# Updates application.yml files with database settings

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DB_CONFIG_FILE="$PROJECT_ROOT/.db-config"
CONFIGS_DIR="$PROJECT_ROOT/configs"

echo "############################################################"
echo "# Database Configuration Setup"
echo "############################################################"

# --- Load or prompt for DB configuration ---
if [ ! -f "$DB_CONFIG_FILE" ]; then
    # First run: input required
    echo ""
    echo ">> Database connection settings (first-time setup)"
    echo ""

    while true; do
        read -p "   Enter database host (e.g., 127.0.0.1 or remote IP): " INPUT_HOST
        if [ -n "$INPUT_HOST" ]; then
            break
        fi
        echo "   ERROR: Database host is required."
    done

    while true; do
        read -p "   Enter database port (default: 5430): " INPUT_PORT
        INPUT_PORT="${INPUT_PORT:-5430}"
        if [ -n "$INPUT_PORT" ]; then
            break
        fi
    done

    while true; do
        read -p "   Enter database username (default: omn): " INPUT_USER
        INPUT_USER="${INPUT_USER:-omn}"
        if [ -n "$INPUT_USER" ]; then
            break
        fi
    done

    while true; do
        read -p "   Enter database password (default: omn): " INPUT_PASSWORD
        INPUT_PASSWORD="${INPUT_PASSWORD:-omn}"
        if [ -n "$INPUT_PASSWORD" ]; then
            break
        fi
    done
else
    # Re-run: read stored values and allow updating
    source "$DB_CONFIG_FILE"

    # Check if DB connection info exists (not just INSTALL_DB)
    if [ -z "${DB_HOST:-}" ]; then
        # DB connection info not saved yet - treat as first run
        echo ""
        echo ">> Database connection settings (first-time setup)"
        echo ""

        while true; do
            read -p "   Enter database host (e.g., 127.0.0.1 or remote IP): " INPUT_HOST
            if [ -n "$INPUT_HOST" ]; then
                break
            fi
            echo "   ERROR: Database host is required."
        done

        while true; do
            read -p "   Enter database port (default: 5430): " INPUT_PORT
            INPUT_PORT="${INPUT_PORT:-5430}"
            if [ -n "$INPUT_PORT" ]; then
                break
            fi
        done

        while true; do
            read -p "   Enter database username (default: omn): " INPUT_USER
            INPUT_USER="${INPUT_USER:-omn}"
            if [ -n "$INPUT_USER" ]; then
                break
            fi
        done

        while true; do
            read -p "   Enter database password (default: omn): " INPUT_PASSWORD
            INPUT_PASSWORD="${INPUT_PASSWORD:-omn}"
            if [ -n "$INPUT_PASSWORD" ]; then
                break
            fi
        done
    else
        # DB connection info exists - allow update
        echo ""
        echo ">> Current database settings:"
        echo "   Host: $DB_HOST"
        echo "   Port: $DB_PORT"
        echo "   Username: $DB_USERNAME"
        echo ""

        read -p "   Press Enter to keep these settings, or type 'change' to update: " CHOICE

        if [ "$CHOICE" = "change" ]; then
            read -p "   Enter database host [$DB_HOST]: " INPUT_HOST
            INPUT_HOST="${INPUT_HOST:-$DB_HOST}"

            read -p "   Enter database port [$DB_PORT]: " INPUT_PORT
            INPUT_PORT="${INPUT_PORT:-$DB_PORT}"

            read -p "   Enter database username [$DB_USERNAME]: " INPUT_USER
            INPUT_USER="${INPUT_USER:-$DB_USERNAME}"

            read -p "   Enter database password: " INPUT_PASSWORD
            INPUT_PASSWORD="${INPUT_PASSWORD:-$DB_PASSWORD}"
        else
            INPUT_HOST="$DB_HOST"
            INPUT_PORT="$DB_PORT"
            INPUT_USER="$DB_USERNAME"
            INPUT_PASSWORD="$DB_PASSWORD"
        fi
    fi
fi

# Save configuration (preserve INSTALL_DB if it exists)
EXISTING_INSTALL_DB=""
if [ -f "$DB_CONFIG_FILE" ]; then
    EXISTING_INSTALL_DB=$(grep "^INSTALL_DB=" "$DB_CONFIG_FILE" | cut -d'=' -f2 || echo "")
fi

cat > "$DB_CONFIG_FILE" <<EOF
DB_HOST=$INPUT_HOST
DB_PORT=$INPUT_PORT
DB_USERNAME=$INPUT_USER
DB_PASSWORD=$INPUT_PASSWORD
EOF

# Restore INSTALL_DB if it existed
if [ -n "$EXISTING_INSTALL_DB" ]; then
    echo "INSTALL_DB=$EXISTING_INSTALL_DB" >> "$DB_CONFIG_FILE"
fi

echo "   [OK] Database config saved to .db-config"
echo ""

read -p "   Enter spring.profiles.active [dev]: " INPUT_PROFILE
INPUT_PROFILE="${INPUT_PROFILE:-dev}"
echo ""

# --- Define servers that use databases ---
SERVERS=("tas" "cas" "issuer" "verifier" "wallet" "op" "lss")

# --- Update application.yml files ---
echo ">> Updating application.yml files with database settings..."
echo ""

for server in "${SERVERS[@]}"; do
    db_name="$server"
    config_file="$CONFIGS_DIR/$server/application.yml"

    if [ ! -f "$config_file" ]; then
        echo "   [SKIP] $server - Config file not found"
        continue
    fi

    # Check if database config already exists
    if grep -q "datasource:" "$config_file"; then
        # Database config exists - remove old config and add new one
        # Backup original file
        cp "$config_file" "${config_file}.bak"

        # Use awk to remove existing database config and add new one
        awk -v host="$INPUT_HOST" -v port="$INPUT_PORT" -v dbname="$db_name" -v user="$INPUT_USER" -v pass="$INPUT_PASSWORD" '
        BEGIN { in_spring=0; in_db_section=0; spring_indent="" }
        /^spring:/ {
            in_spring=1
            print
            next
        }
        in_spring && /^  [a-z]/ {
            # Check if this is start of db config section
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
            # End of db section, start of new top-level spring property
            in_db_section=0
            in_spring=0
            print
            next
        }
        in_db_section {
            # Skip lines that are part of db config
            next
        }
        !in_db_section {
            print
        }
        ' "${config_file}.bak" > "$config_file"

        echo "   [UPDATE] $server - Database config updated"
    else
        # Database config does not exist - add after spring.profiles
        # Backup original file
        cp "$config_file" "${config_file}.bak"

        # Insert database config after spring.profiles.active line
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

        echo "   [ADD] $server - Database config added"
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
echo "############################################################"
echo "# Database configuration complete!"
echo "############################################################"
