#!/usr/bin/env bash
set -euo pipefail

# Minimal backup script for PostgreSQL with optional upload to S3.
# Environment variables used (provide via CI / k8s Secret):
#  DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
#  BACKUP_DIR (local staging dir, default /tmp/backups)
#  S3_BUCKET (optional) - if provided, script uploads backup to s3://$S3_BUCKET/
#  AWS_PROFILE / AWS_REGION (optional)
#  RETENTION_DAYS (optional, default 30)

BACKUP_DIR=${BACKUP_DIR:-/tmp/backups}
S3_BUCKET=${S3_BUCKET:-}
RETENTION_DAYS=${RETENTION_DAYS:-30}
TIMESTAMP=$(date -u +"%Y%m%dT%H%M%SZ")
FNAME="sinatra_backup_${TIMESTAMP}.sql.gz"
LOCAL_PATH="$BACKUP_DIR/$FNAME"

mkdir -p "$BACKUP_DIR"

# Prefer explicit DB_PASSWORD, fallback to PGPASSWORD env
export PGPASSWORD="${DB_PASSWORD:-${PGPASSWORD:-}}"

echo "[backup] running pg_dump to $LOCAL_PATH"
pg_dump -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-sinatra_user}" -F p "${DB_NAME:-sinatra_prod}" | gzip > "$LOCAL_PATH"

if [ -n "$S3_BUCKET" ]; then
  echo "[backup] uploading to s3://$S3_BUCKET/$FNAME"
  if command -v aws >/dev/null 2>&1; then
    aws s3 cp "$LOCAL_PATH" "s3://$S3_BUCKET/$FNAME"
  else
    echo "[backup] aws cli not found, skipping upload" >&2
  fi
fi

echo "[backup] cleaning local backups older than $RETENTION_DAYS days"
find "$BACKUP_DIR" -type f -name 'sinatra_backup_*.sql.gz' -mtime +$RETENTION_DAYS -delete || true

echo "[backup] done: $LOCAL_PATH"
