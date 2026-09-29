# Future Bound Tech — Deployment Guide (Phase 27)

Production deployment reference for the Spring Boot + Thymeleaf LMS.
Companion files: [`../README.md`](../README.md) (project + API overview), everything under [`deploy/`](.).

> **Ground rules enforced by the `prod` profile** (`ProductionValidator` refuses startup otherwise):
> real Razorpay **live** keys, `https://` base URL, no demo seeding with a weak admin password,
> no H2 datasource. Secrets are read from **environment variables only** — see [`.env.example`](../.env.example).

---

## 1. Local development

```powershell
# Windows PowerShell (from the repository root)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.1"
.\mvnw.cmd spring-boot:run          # H2 file DB, demo mode, http://localhost:8080
```

* Profile `h2` is the default: embedded database in `./data/`, H2 console at `/h2-console`, seeders on.
* MySQL for local testing: create `future_bound_tech_db`, then
  `$env:DB_PASSWORD="..."; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=mysql"`.
* Tests: `.\mvnw.cmd clean package` runs the full suite (65 tests) and produces the deployable jar.
* Payments run in **demo mode** locally (`RAZORPAY_ENABLED` unset/false): orders settle server-side, no gateway calls.

## 2. Requirements

| Component | Minimum | Notes |
| --- | --- | --- |
| JDK | 17 | Temurin/Oracle; `java -version` must report 17 |
| MySQL | 8.0 | utf8mb4; InnoDB |
| nginx | 1.20+ | reverse proxy + TLS termination |
| Memory | 1 GB free | unit starts with `-Xmx1g` |

## 3. Build the artifact

```bash
./mvnw.cmd clean package            # Windows PowerShell (or ./mvnw on Linux)
# → target/future-bound-tech-1.0.0.jar  (executable, tests included in the run)
```

Copy the jar to the server: `/opt/future-bound-tech/future-bound-tech-1.0.0.jar`.
Keep a copy of the **previous** jar as `...jar.prev` for rollback (§12).

## 4. MySQL setup

Run [`deploy/mysql-setup.sql`](mysql-setup.sql) as root after replacing the `<...>` password
placeholders with values from your secret store (`openssl rand -base64 24`):

```bash
sudo mysql < /etc/future-bound-tech/mysql-setup.sql   # filled-in copy lives OUTSIDE the repo
```

It creates:
* database `future_bound_tech_db` (utf8mb4/utf8mb4_unicode_ci)
* `fbt_app` — the runtime user (CRUD + schema evolution for `ddl-auto=update`)
* `fbt_backup` — read-only export user for the backup cron

The prod JDBC URL uses `useSSL=true&requireSSL=true&verifyServerCertificate=true`.
MySQL 8 enables SSL by way of its auto-generated certs, so this works out of the box on a
fresh install; for a managed DB, point `DB_HOST` at the provider's TLS endpoint. If your
deployment co-locates MySQL on the same server and you must opt out of TLS, override
`spring.datasource.url` in the env file — accept the risk explicitly, don't do it silently.

## 5. Environment variables

Copy [`.env.example`](../.env.example) to `/etc/future-bound-tech.env`, fill it in, lock it down:

```bash
sudo install -o root -g fbt -m 600 /dev/null /etc/future-bound-tech.env
sudoedit /etc/future-bound-tech.env
sudo chmod 600 /etc/future-bound-tech.env
```

Key variables (full list in the template):

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | activates the production profile (implies `mysql`) |
| `APP_BASE_URL` | your HTTPS origin, e.g. `https://<your-domain>` — used on certificate links and callbacks |
| `DB_HOST/DB_NAME/DB_USERNAME/DB_PASSWORD` | MySQL connection; **no default password exists** in prod |
| `RAZORPAY_KEY_ID/KEY_SECRET/WEBHOOK_SECRET` | live gateway credentials (§9) |
| `BOOTSTRAP_SEEDS` | `true` only for the very first start, then `false` |
| `ADMIN_SEED_EMAIL/ADMIN_SEED_PASSWORD` | bootstrap admin (strong password or startup is refused) |
| `UPLOAD_DIR`/`PRIVATE_UPLOAD_DIR` | persistent storage outside the web root |
| `LOG_FILE` | rolling application log location |
| `APP_HSTS_ENABLED` | `true` once HTTPS is confirmed working (§13) |

## 6. Application startup (systemd)

```bash
sudo useradd -r -m -d /opt/future-bound-tech -s /usr/sbin/nologin fbt
sudo mkdir -p /var/log/future-bound-tech /var/backups/future-bound-tech
sudo chown fbt:fbt /var/log/future-bound-tech
sudo cp deploy/future-bound-tech.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now future-bound-tech
systemctl status future-bound-tech
```

**First start:** set `BOOTSTRAP_SEEDS=true` + `ADMIN_SEED_*`. The seeder creates the catalog
and the admin, then **remove those variables** (`BOOTSTRAP_SEEDS=false`) and restart, so
demo content/students can never be (re)seeded on a live database.

Manual one-off run (debugging):

```bash
sudo -u fbt env $(grep -v '^#' /etc/future-bound-tech.env | xargs -d '\n') \
    java -jar /opt/future-bound-tech/future-bound-tech-1.0.0.jar
```

## 7. Reverse proxy, HTTPS, domain

1. **Domain:** point an **A record** for your real domain (this guide deliberately uses
   the `<your-domain>` placeholder — never deploy against an invented name) to the server.
2. Install [`deploy/nginx.conf`](nginx.conf) to `sites-available`, symlink it, replace `<your-domain>`.
3. Certificate via Let's Encrypt:
   ```bash
   sudo apt install certbot python3-certbot-nginx
   sudo certbot --nginx -d <your-domain>       # also rewrites 80→443 redirect
   sudo certbot renew --dry-run                # auto-renewal is installed by the package
   ```
4. `sudo nginx -t && sudo systemctl reload nginx`.
5. Open **only** 80/4443→ proxy; keep 8080 bound to localhost (the proxy passes to `127.0.0.1`).
6. Once HTTPS is verified, the app sends **HSTS** automatically under `prod`
   (`APP_HSTS_ENABLED=true`) — browsers will then refuse plain HTTP for a year, so test
   the HTTPS path thoroughly before enabling.

## 8. Frontend / static assets in production

* Bootstrap 5.3 CSS/JS + icons load from the **jsDelivr CDN with SRI hashes**
  (`templates/fragments/head.html`, `scripts.html`) — they work over HTTPS by themselves;
  no local bundling step exists (there is no Node build).
* App CSS/JS/images live in `src/main/resources/static/` and are served from inside the jar.
* Uploaded images (branding, course thumbnails, trainer photos, avatars) are served from
  `UPLOAD_DIR` at `/uploads/<folder>/**` via `WebConfig` — **the folder must exist and be
  owned by `fbt`**, and backups must include it (§10). Private files (lesson notes,
  submissions) live under `PRIVATE_UPLOAD_DIR` and are only reachable through
  authenticated, ownership-checked endpoints.
* `spring.thymeleaf.cache=true` and `spring.web.resources.cache.period` are on under `prod`;
  after a **frontend-only** deploy, hard-refresh (Ctrl+F5) to rule out stale caches.
* Responsive behaviour is CSS-driven (Bootstrap grid + the `navbar-toggler` mobile menu) —
  verified in Phase 24/26 QA; nothing profile-specific changes layout.

## 9. Razorpay configuration

| Mode | How |
| --- | --- |
| **Test** (staging only) | Dashboard → Settings → API Keys (test mode). Set `RAZORPAY_KEY_ID=rzp_test_…`, `RAZORPAY_KEY_SECRET=…`, `RAZORPAY_WEBHOOK_SECRET=…`, `RAZORPAY_ENABLED=true`, and `APP_BASE_URL=https://<your-domain>` on a **public HTTPS** staging host (Razorpay webhooks/checkout cannot reach `localhost`). `ProductionValidator` **blocks `rzp_test_` keys under the `prod` profile** — stage with profile `mysql` instead. |
| **Production** | Same three variables with **live** keys (`rzp_live_…`). Never reuse a test secret. Rotate immediately if a secret was ever pasted into chat/CI logs. |

* **Webhook:** register `https://<your-domain>/api/payments/webhook` in the Razorpay
  dashboard, events `payment.captured`, `payment.failed`, `refund.created`, `refund.processed`,
  with the same secret you put in `RAZORPAY_WEBHOOK_SECRET`. The endpoint is permitAll
  (server-to-server) and verifies **HMAC-SHA256 of the raw body** against that secret with a
  constant-time compare before processing; replays are idempotent.
* **Checkout signature** (`/api/payments/verify`): server recomputes `HMAC(order_id|payment_id, key_secret)` —
  the client can never influence amounts (fees are always re-quoted server-side).
* Webhook deliveries appear in the dashboard under *Webhooks → Events*; a `200 ok` response
  from the app acknowledges delivery.

## 10. Backup & restore

| What | Command |
| --- | --- |
| Backup (Linux cron) | `15 2 * * * /opt/future-bound-tech/deploy/backup.sh` — dumps DB (`mysqldump --single-transaction`) **and** archives both upload folders, 30-day retention |
| Backup (Windows) | `.\deploy\backup.ps1` (Task Scheduler, daily 02:15) |
| Restore | `./deploy/restore.sh /var/backups/future-bound-tech/db-<stamp>.sql.gz` (typing `RESTORE` required), then untar the uploads archives |
| Off-site copy | replicate `BACKUP_DIR` (object storage / second server). A backup on the same disk is not a backup. |

**Test a restore quarterly** on a scratch instance — an untested dump is not a backup.

## 11. Database migration strategy

Current approach: **Hibernate `ddl-auto=update`** (`JPA_DDL_AUTO` env var). It adds new
tables/columns/indexes automatically and is safe for additive changes.

Rules and upgrade path:

1. `update` **never drops** columns/tables and never renames anything — leftover columns are
   cosmetic debt, not corruption. Reclaim them with a manual `ALTER TABLE ... DROP COLUMN`
   after confirming no data is needed.
2. **Before every release that changes entities:** take a full backup (§10). The diff Hibernate
   applies is printed in the startup log at DEBUG — capture it with the release notes.
3. Dangerous changes (type changes, non-nullable columns on populated tables) must be done as
   a scripted, manual migration: add column nullable → backfill → switch entity → drop old.
4. **Recommended next step:** introduce Flyway (`org.flywaydb:flyway-core` +
   `spring.flyway.enabled=true`, baseline on the existing schema, then `ddl-auto=validate`).
   Deliberately not switched on here, because the live DB baseline and migration scripts must
   be authored against your real schema snapshot first — flipping it blind would fail startup.
5. To set the stricter mode without Flyway: `JPA_DDL_AUTO=validate` after confirming the
   entities and schema agree (start once with `update`, then switch).

## 12. Release & rollback

```bash
# Release
systemctl stop future-bound-tech
cp /opt/future-bound-tech/future-bound-tech-1.0.0.jar{,.prev}
scp target/future-bound-tech-1.0.0.jar server:/opt/future-bound-tech/
systemctl start future-bound-tech && journalctl -u future-bound-tech -f   # watch for "Started ... in"
curl -fsS https://<your-domain>/api/health

# Rollback (schema is additive, so the old jar still works against the new DB)
systemctl stop future-bound-tech
mv /opt/future-bound-tech/future-bound-tech-1.0.0.jar{.prev,}
systemctl start future-bound-tech
```

* Additive-only schema changes keep rollback one-step; anything that *consumed* new columns
  (a release that wrote data the old version can't read) requires restoring the §10 backup.
* Uploaded files are version-agnostic — no rollback action for `UPLOAD_DIR`.

## 13. Logs & monitoring

| Source | Location |
| --- | --- |
| Application | `LOG_FILE` (`/var/log/future-bound-tech/app.log`), rolled at 20 MB, 30 days, 1 GB cap |
| systemd | `journalctl -u future-bound-tech [-f] [--since today]` |
| nginx | `/var/log/nginx/future-bound-tech.{access,error}.log` |

Health probe for external monitors: `GET /api/health` → `{"status":"UP",...}` (public, no auth).
Alert on: process restarts (`Restart=on-failure` in the unit), `/api/health` failures,
log line `Refusing to serve production traffic` (misconfiguration), webhook `Rejected Razorpay
webhook` spikes (wrong secret or attacks).

## 14. Troubleshooting

| Symptom | Cause → fix |
| --- | --- |
| Startup: `Refusing to serve production traffic …` | `ProductionValidator` tripped — the log lists exactly which variable is placeholder/missing/test-key. Fix env file, restart. |
| Startup: `Failed to determine a suitable driver class` / H2 error | `SPRING_PROFILES_ACTIVE` not picked up → ensure `prod` (it groups `mysql`) and `DB_*` are in `/etc/future-bound-tech.env`. |
| `Access denied for user 'fbt_app'` | Wrong password or user created for `'localhost'` only while app runs elsewhere → check `SELECT host,user FROM mysql.user`, recreate with `'%'` or the real host. |
| `SSL error: Authentication requires secure connection` | JDBC requires TLS but MySQL lacks certs → `sudo mysql_config_editor`/enable auto certs, or override the URL per §4 (documented risk). |
| Login works over HTTP but not HTTPS, redirect loops | Missing `X-Forwarded-Proto` at the proxy → use `deploy/nginx.conf` as-is; app needs `server.forward-headers-strategy=native` (on by default under prod). |
| Checkout opens but fails to load | Public URL must be HTTPS and reachable from the browser; verify `RAZORPAY_KEY_ID` matches the mode (test/live) of the account issuing payments. |
| Payments capture but enrollment stuck PENDING | Webhook not arriving/secret mismatch → check dashboard event log, `RAZORPAY_WEBHOOK_SECRET` equality, nginx `client_max_body_size`, path `/api/payments/webhook`. The `/api/payments/verify` path also finalises on its own. |
| Mobile menu / styling broken | CDN blocked (ad-block/network)? SRI mismatch means the browser refuses the file → check console; only update hashes together with the version in `fragments/head.html`. |
| Images 404 after deploy | `UPLOAD_DIR` points somewhere new/empty → the folder must contain prior uploads; restore from backup (§10). |
| Template changes ignored | Expected: `thymeleaf.cache=true` in prod → redeploy the jar (templates ship inside it). |
| 502 at nginx, app log silent | App not listening on `SERVER_PORT` or unit user can't write `LOG_FILE` → `systemctl status`, `ss -ltnp`, ownership of `/var/log/future-bound-tech`. |

## 15. Security checklist (pre-launch)

- [ ] All defaults from `.env.example` replaced; file is `chmod 600`, never committed (`.env` is git-ignored)
- [ ] Live Razorpay keys only; webhook secret set identically in dashboard and env
- [ ] `BOOTSTRAP_SEEDS=false` + demo admin password changed (or `ADMIN_SEED_EMAIL` is yours)
- [ ] HTTPS serving real traffic, HSTS enabled, HTTP redirects at the proxy
- [ ] 8080 not reachable from the internet (firewall); H2 console absent under `prod`
- [ ] Backup cron installed **and** one restore rehearsed
- [ ] Ports/users minimal: `fbt` service account, no root Java process
