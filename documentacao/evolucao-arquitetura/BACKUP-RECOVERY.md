# Backup & Recovery — Sinatra Mini

Objetivo: garantir backups automáticos do banco PostgreSQL com retenção e procedimento de restore documentado.

Resumo da solução proposta:
- Script de backup (`scripts/backup.sh`) que faz `pg_dump` + gzip, com upload opcional para S3.
- Script de restore (`scripts/restore.sh`) que baixa de S3 (se necessário) e restaura via `psql`.
- `CronJob` Kubernetes template em `k8s/backup-cronjob.yaml` para execução diária.
- Uso de `Secrets` para credenciais (`sinatra-secrets`): `db_user`, `db_password`, `backups_s3_bucket`.

RTO/RPO targets (proposta):
- RTO (Recovery Time Objective): < 30 minutos (restore testado em staging)
- RPO (Recovery Point Objective): < 1 hora (backups diários ou por hora dependendo do SLA)

Configuração rápida (local / dev):
1. Criar bucket S3 e armazenar nome em `S3_BUCKET` ou em k8s secret `backups_s3_bucket`.
2. Exportar variáveis e executar manualmente:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=sinatra_prod
export DB_USER=sinatra_user
export DB_PASSWORD=secret
export S3_BUCKET=my-sinatra-backups
./scripts/backup.sh
```

Restore manual:

```bash
# local file
./scripts/restore.sh /tmp/backups/sinatra_backup_20260205T020000Z.sql.gz

# from s3
./scripts/restore.sh s3://my-sinatra-backups/sinatra_backup_20260205T020000Z.sql.gz
```

Kubernetes: criar `Secret` com as chaves e aplicar `k8s/backup-cronjob.yaml` (ajuste `DB_HOST` para service correto). Exemplo:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: sinatra-secrets
  namespace: sinatra-mini
stringData:
  db_user: sinatra_user
  db_password: YOUR_DB_PASSWORD
  backups_s3_bucket: my-sinatra-backups
```

Próximos passos recomendados:
- Testar restore em ambiente de staging (marcar como tarefa obrigatória antes de marcar Issue #19 como DONE).
- Implementar CronJob com políticas de retenção server-side (lifecycle) no S3.
- Adicionar monitoramento/alertas para falha de backup (hook para Slack/PagerDuty).
