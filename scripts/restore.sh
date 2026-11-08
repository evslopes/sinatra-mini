#!/usr/bin/env bash
set -euo pipefail

# Restore script for PostgreSQL backups produced by scripts/backup.sh
# Usage: restore.sh <source>
#  where <source> is either a local path (/tmp/backups/...) or s3://bucket/key
# Env vars required for DB connection: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <backup-file-or-s3-url>" >&2
  exit 2
fi

SRC="$1"
TMP_DIR=$(mktemp -d)
LOCAL_FILE="$TMP_DIR/restore.sql.gz"

if [[ "$SRC" == s3://* ]]; then
  if ! command -v aws >/dev/null 2>&1; then
    echo "aws cli not found; cannot download $SRC" >&2
    exit 1
  fi
  echo "[restore] downloading $SRC"
  aws s3 cp "$SRC" "$LOCAL_FILE"
else
  echo "[restore] copying local file $SRC"
  cp "$SRC" "$LOCAL_FILE"
fi

echo "[restore] decompressing and restoring to ${DB_NAME:-sinatra_prod}"
export PGPASSWORD="${DB_PASSWORD:-${PGPASSWORD:-}}"
gzip -dc "$LOCAL_FILE" | psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-sinatra_user}" -d "${DB_NAME:-sinatra_prod}"

echo "[restore] done"
