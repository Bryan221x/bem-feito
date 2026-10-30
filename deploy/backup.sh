#!/bin/sh
# Linux VPS; run from the repository root with Compose variables already exported.
# A short maintenance window keeps the database and uploads consistent.
set -eu
umask 077
: "${BACKUP_DIR:?Set an absolute backup directory outside the repository}"
case "$BACKUP_DIR" in /*) ;; *) echo 'BACKUP_DIR must be absolute' >&2; exit 1;; esac
mkdir -p "$BACKUP_DIR"
backup_root=$(cd "$BACKUP_DIR" && pwd -P)
repo_root=$(pwd -P)
case "$backup_root/" in "$repo_root/"*) echo 'Backups must be outside the repository' >&2; exit 1;; esac
destination="$backup_root/$(date -u +%Y%m%dT%H%M%SZ)"
mkdir "$destination"
running=$(docker compose ps --status running --services)
resume() {
    for service in api frontend; do
        if printf '%s\n' "$running" | grep -qx "$service"; then docker compose start "$service" >/dev/null; fi
    done
}
trap resume EXIT
docker compose stop frontend api
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump --user=root --single-transaction --routines --triggers --events --hex-blob --no-tablespaces --set-gtid-purged=OFF bem_feito' > "$destination/database.sql"
docker compose run --rm --no-deps -T --entrypoint tar api -C /var/lib/bemfeito/uploads -cf - . > "$destination/uploads.tar"
test -s "$destination/database.sql"
test -s "$destination/uploads.tar"
(cd "$destination" && sha256sum database.sql uploads.tar > SHA256SUMS)
printf '%s\n' 'Backup complete; encrypt and copy off-host before applying retention.'
