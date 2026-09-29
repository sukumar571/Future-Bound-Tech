#!/usr/bin/env bash
# ================================================================
# Future Bound Tech — MySQL backup (Linux production)
# Usage:      ./deploy/backup.sh
# Schedule:   crontab -e  ->  15 2 * * * /opt/future-bound-tech/deploy/backup.sh
#
# Credentials come from environment variables, never from a file in
# this repository:
#   DB_HOST (default localhost), DB_PORT (3306), DB_NAME (future_bound_tech_db)
#   BACKUP_DB_USERNAME (default fbt_backup), BACKUP_DB_PASSWORD (required)
#
# The dump also covers the uploaded files, which live OUTSIDE the DB —
# a database backup alone can never restore lesson PDFs or avatars.
# ================================================================
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-future_bound_tech_db}"
BACKUP_DB_USERNAME="${BACKUP_DB_USERNAME:-fbt_backup}"
: "${BACKUP_DB_PASSWORD:?BACKUP_DB_PASSWORD must be exported before running backup.sh}"

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/future-bound-tech}"
UPLOAD_DIR="${UPLOAD_DIR:-/var/opt/future-bound-tech/uploads}"
PRIVATE_UPLOAD_DIR="${PRIVATE_UPLOAD_DIR:-/var/opt/future-bound-tech/private-uploads}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

mkdir -p "$BACKUP_DIR"

# 1. Schema + data. --single-transaction keeps InnoDB consistent without
#    locking; --set-gtid-purged=OFF makes the dump restorable on fresh hosts.
MYSQL_PWD="$BACKUP_DB_PASSWORD" mysqldump \
    -h "$DB_HOST" -P "$DB_PORT" -u "$BACKUP_DB_USERNAME" \
    --single-transaction --routines --triggers --events \
    --set-gtid-purged=OFF --default-character-set=utf8mb4 \
    "$DB_NAME" | gzip > "$BACKUP_DIR/db-$STAMP.sql.gz"

# 2. Uploaded files (public + private), if the folders exist.
for dir in "$UPLOAD_DIR" "$PRIVATE_UPLOAD_DIR"; do
    if [ -d "$dir" ]; then
        tar -czf "$BACKUP_DIR/$(basename "$dir")-$STAMP.tar.gz" -C "$(dirname "$dir")" "$(basename "$dir")"
    fi
done

# 3. Retention: delete everything older than $RETENTION_DAYS.
find "$BACKUP_DIR" -name '*.sql.gz' -mtime "+$RETENTION_DAYS" -delete
find "$BACKUP_DIR" -name '*.tar.gz' -mtime "+$RETENTION_DAYS" -delete

echo "Backup written to $BACKUP_DIR (stamp $STAMP)."
