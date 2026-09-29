#!/usr/bin/env bash
# ================================================================
# Future Bound Tech — MySQL restore (Linux)
# Usage:  ./deploy/restore.sh /var/backups/future-bound-tech/db-YYYYmmdd-HHMMSS.sql.gz
#
# DANGER: this OVERWRITES the target database. The script refuses to
# run without an explicit "RESTORE" confirmation.
#
# Env: DB_HOST, DB_PORT, DB_NAME, RESTORE_DB_USERNAME (default fbt_app),
#      RESTORE_DB_PASSWORD (required — or use a root account for drops).
# ================================================================
set -euo pipefail

DUMP="${1:?Usage: restore.sh <db-....sql.gz>}"
[ -f "$DUMP" ] || { echo "Dump file not found: $DUMP"; exit 1; }

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-future_bound_tech_db}"
RESTORE_DB_USERNAME="${RESTORE_DB_USERNAME:-fbt_app}"
: "${RESTORE_DB_PASSWORD:?RESTORE_DB_PASSWORD must be exported before running restore.sh}"

echo "About to DESTROY and REBUILD database '$DB_NAME' on $DB_HOST:$DB_PORT from:"
echo "  $DUMP"
read -r -p "Type RESTORE to continue: " CONFIRM
[ "$CONFIRM" = "RESTORE" ] || { echo "Aborted."; exit 1; }

export MYSQL_PWD="$RESTORE_DB_PASSWORD"

# Recreate the schema from scratch so a half-restored state is impossible.
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$RESTORE_DB_USERNAME" \
    -e "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

gunzip -c "$DUMP" | mysql -h "$DB_HOST" -P "$DB_PORT" -u "$RESTORE_DB_USERNAME" "$DB_NAME"

echo "Restored $DUMP into $DB_NAME. Restart the application afterwards."
# Uploaded files are restored separately, e.g.:
#   tar -xzf uploads-YYYYmmdd-HHMMSS.tar.gz -C /var/opt/future-bound-tech/
