-- ================================================================
-- Future Bound Tech — MySQL production setup
-- Run as a MySQL administrator (root):
--   mysql -u root -p < deploy/mysql-setup.sql
--
-- IMPORTANT: replace the <...> placeholders below with strong
-- generated secrets before running. Never commit the edited copy
-- (keep your filled-in variant outside the repository, e.g. in the
-- server's /etc/future-bound-tech/ directory).
--   Generate one:  openssl rand -base64 24
-- ================================================================

-- 1. Database (utf8mb4 so every script and emoji stores correctly)
CREATE DATABASE IF NOT EXISTS future_bound_tech_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 2. Application user — least privilege for normal operation.
--    Hibernate's ddl-auto=update needs CREATE/ALTER/INDEX/DROP on this
--    schema only. If you later freeze the schema (ddl-auto=validate —
--    recommended once migrations are scripted) revoke CREATE, ALTER, DROP.
CREATE USER IF NOT EXISTS 'fbt_app'@'%'
  IDENTIFIED BY '<APP_DB_PASSWORD>';

GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, INDEX, DROP, REFERENCES
  ON future_bound_tech_db.* TO 'fbt_app'@'%';

-- 3. Backup user — read-only export rights for mysqldump
--    (used by backup.sh / BACKUP.bat via MYSQL_BACKUP_PASSWORD).
CREATE USER IF NOT EXISTS 'fbt_backup'@'localhost'
  IDENTIFIED BY '<BACKUP_DB_PASSWORD>';

GRANT SELECT, LOCK TABLES, SHOW VIEW, TRIGGER, PROCESS
  ON future_bound_tech_db.* TO 'fbt_backup'@'localhost';

FLUSH PRIVILEGES;

-- 4. Quick verification (expected: the grants above, nothing more)
--    SHOW GRANTS FOR 'fbt_app'@'%';
--    mysql -h <DB_HOST> -u fbt_app -p'<APP_DB_PASSWORD>' future_bound_tech_db -e 'SELECT 1;'
