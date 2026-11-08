# Agente DevOps - Sinatra Mini

## 1. Missão
Gerenciar o ciclo de vida da aplicação (Build, Test, Deploy), a infraestrutura (PostgreSQL, Redis, Load Balancers) e o versionamento de código (Git/GitHub CLI). Garantir que os ambientes (dev, staging, prod) reflitam as restrições e requisitos de produção (alta disponibilidade, escalabilidade, observabilidade).

## 2. Comandos de Build e Execução (Maven)
O projeto utiliza Maven Wrapper (mvnw).

### 2.1 Build e Testes
- Compilar:
  ```bash
  ./mvnw clean compile
  ```
- Testar:
  ```bash
  ./mvnw test
  ```
- Testar com PostgreSQL (TestContainers):
  ```bash
  ./mvnw -DskipUnitTests=false -Dspring.profiles.active=test-postgres test
  ```
- Build completo com empacotamento:
  ```bash
  ./mvnw clean install
  ```

### 2.2 Execução Local
- Rodar aplicação (perfil dev com H2):
  ```bash
  ./mvnw spring-boot:run
  ```
- Rodar com PostgreSQL local:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-postgres
  ```
- Rodar com perfil de produção (simula restrições):
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
  ```

### 2.3 Flyway Migrations
- Aplicar migrations localmente:
  ```bash
  ./mvnw -Dflyway.configFiles=src/main/resources/flyway.conf flyway:migrate
  ```
- Validar migrations:
  ```bash
  ./mvnw flyway:validate
  ```
- Rollback (via scripts manuais - documentar em migrations/):
  ```bash
  psql -U sinatra_user -d sinatra_dev < src/main/resources/db/migration/rollback/R__undo_v2.sql
  ```

## 3. Gestão de Repositório (GitHub CLI - gh)

### 3.1 Autenticação e Configuração
- Login (se necessário):
  ```bash
  gh auth login
  ```
- Status do repositório:
  ```bash
  gh status
  ```

### 3.2 Fluxo de Trabalho (Issues e PRs)

#### Criar Issue (Task do Agile):
```bash
gh issue create --title "Story 1.1.1: PostgreSQL Setup" \
  --body "Configurar PostgreSQL em dev, staging e prod" \
  --label "epic:postgresql,priority:critical" \
  --milestone "v1.1.0"
```

#### Listar Issues
```bash
# Ver todas as issues abertas
gh issue list --state open

# Filtrar por épico
gh issue list --label "epic:postgresql"

# Filtrar por milestone
gh issue list --milestone "v1.1.0"
```

#### Trabalhar em uma Issue
```bash
# Ver detalhes
gh issue view 16

# Atribuir a si mesmo
gh issue edit 16 --add-assignee @me

# Criar branch a partir da issue
git checkout develop
git pull origin develop
git checkout -b feature/1-postgresql-setup
```

#### Pull Requests
```bash
# Criar PR vinculado à issue
gh pr create --title "feat: PostgreSQL setup #16" \
  --body "Closes #16" \
  --base develop \
  --head feature/1-postgresql-setup

# Revisar PR
gh pr review 10 --approve

# Merge PR
gh pr merge 10 --squash
```

---

## 4. Épicos de Infraestrutura (Roadmap v1.1.0 - v1.2.0)

### 4.1 Épico 1: PostgreSQL Setup (v1.1.0)

**Issue GitHub:** [#15](https://github.com/evslopes/sinatra-mini/issues/15)  
**Responsável:** DevOps Agent  
**Stories:** [#16](https://github.com/evslopes/sinatra-mini/issues/16), [#19](https://github.com/evslopes/sinatra-mini/issues/19)

#### Story 1.1.1: Setup PostgreSQL e Configuração de Ambientes

**Artefatos:**
```yaml
# docker-compose.yml (desenvolvimento)
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    container_name: sinatra_postgres_dev
    environment:
      POSTGRES_DB: sinatra_dev
      POSTGRES_USER: sinatra_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U sinatra_user"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: sinatra_redis_dev
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

volumes:
  postgres_data:
  redis_data:
```

**Comandos:**
```bash
# Subir infraestrutura local
docker-compose up -d

# Verificar status
docker-compose ps

# Logs
docker-compose logs -f postgres

# Conectar ao PostgreSQL
docker exec -it sinatra_postgres_dev psql -U sinatra_user -d sinatra_dev

# Parar infraestrutura
docker-compose down
```

**Profiles Spring:**
```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sinatra_dev
    username: sinatra_user
    password: ${DB_PASSWORD:dev_password}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: validate  # NUNCA 'update' em prod
```

**Checklist:**
- [ ] PostgreSQL 15+ instalado em todos os ambientes
- [ ] Databases criados: `sinatra_dev`, `sinatra_staging`, `sinatra_prod`
- [ ] Usuários e permissões configurados (princípio do menor privilégio)
- [ ] Variáveis de ambiente documentadas (`.env.example`)
- [ ] Smoke tests executados com sucesso

---

#### Story 1.1.4: Backup e Recovery

**Cron Script (Linux):**
```bash
#!/bin/bash
# /etc/cron.daily/postgres-backup.sh

BACKUP_DIR="/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="sinatra_prod"

# Backup completo
pg_dump -U sinatra_user -h localhost $DB_NAME | gzip > \
    $BACKUP_DIR/sinatra_backup_$DATE.sql.gz

# Upload para S3/Azure Blob
aws s3 cp $BACKUP_DIR/sinatra_backup_$DATE.sql.gz \
    s3://sinatra-backups/postgres/

# Limpar backups antigos (> 7 dias)
find $BACKUP_DIR -type f -mtime +7 -delete

# Notificar sucesso
echo "Backup completed: sinatra_backup_$DATE.sql.gz"
```

**Restore:**
```bash
# Download do backup
aws s3 cp s3://sinatra-backups/postgres/sinatra_backup_20260121_030000.sql.gz /tmp/

# Restore
gunzip < /tmp/sinatra_backup_20260121_030000.sql.gz | \
    psql -U sinatra_user -d sinatra_prod
```

**RTO/RPO:**
- **RPO (Recovery Point Objective):** 24 horas
- **RTO (Recovery Time Objective):** 2 horas

---

### 4.2 Épico 2: Cache Distribuído - Redis (v1.1.0)

**Issue GitHub:** [#24](https://github.com/evslopes/sinatra-mini/issues/24)  
**Responsável:** DevOps Agent (setup) + Engineer Agent (integração)

#### Redis Setup (Docker)

**docker-compose.yml (Redis Cluster - produção):**
```yaml
version: '3.8'
services:
  redis-master:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --maxmemory 256mb --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    volumes:
      - redis_master_data:/data

  redis-replica1:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --replicaof redis-master 6379 --masterauth ${REDIS_PASSWORD}
    depends_on:
      - redis-master
    volumes:
      - redis_replica1_data:/data

  redis-replica2:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD} --replicaof redis-master 6379 --masterauth ${REDIS_PASSWORD}
    depends_on:
      - redis-master
    volumes:
      - redis_replica2_data:/data

volumes:
  redis_master_data:
  redis_replica1_data:
  redis_replica2_data:
```

**Monitoramento:**
```bash
# Ver estatísticas do Redis
docker exec -it sinatra_redis_master redis-cli --pass ${REDIS_PASSWORD} INFO stats

# Monitor em tempo real
docker exec -it sinatra_redis_master redis-cli --pass ${REDIS_PASSWORD} MONITOR

# Ver keys em cache
docker exec -it sinatra_redis_master redis-cli --pass ${REDIS_PASSWORD} KEYS '*'

# Limpar cache (cuidado!)
docker exec -it sinatra_redis_master redis-cli --pass ${REDIS_PASSWORD} FLUSHALL
```

---

### 4.3 Épico 3: Alta Disponibilidade (v1.2.0)

**Issue GitHub:** [#26](https://github.com/evslopes/sinatra-mini/issues/26)  
**Responsável:** DevOps Agent

#### Story 1.2.1: Load Balancer (Nginx)

**nginx.conf:**
```nginx
upstream sinatra_backend {
    least_conn;  # Algoritmo: menor número de conexões
    
    server app1:8080 max_fails=3 fail_timeout=30s;
    server app2:8081 max_fails=3 fail_timeout=30s;
    server app3:8082 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.sinatra.com;
    
    location / {
        proxy_pass http://sinatra_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # Health check
        proxy_next_upstream error timeout http_500;
        
        # Timeouts
        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    location /health {
        access_log off;
        proxy_pass http://sinatra_backend/actuator/health;
    }
}
```

**docker-compose.yml (Multi-Instance):**
```yaml
version: '3.8'
services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - app1
      - app2
      - app3

  app1:
    image: sinatra-mini:latest
    environment:
      SERVER_PORT: 8080
      SPRING_PROFILES_ACTIVE: prod
    depends_on:
      - postgres
      - redis

  app2:
    image: sinatra-mini:latest
    environment:
      SERVER_PORT: 8081
      SPRING_PROFILES_ACTIVE: prod
    depends_on:
      - postgres
      - redis

  app3:
    image: sinatra-mini:latest
    environment:
      SERVER_PORT: 8082
      SPRING_PROFILES_ACTIVE: prod
    depends_on:
      - postgres
      - redis
```

---

### 4.4 Épico 4: Observabilidade (v1.2.0)

**Issue GitHub:** [#30](https://github.com/evslopes/sinatra-mini/issues/30)  
**Responsável:** DevOps Agent

#### Story 1.2.3: Stack de Observabilidade

**docker-compose.yml (Observability Stack):**
```yaml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=15d'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
    depends_on:
      - prometheus

  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # UI
      - "14268:14268"  # Collector
    environment:
      COLLECTOR_ZIPKIN_HOST_PORT: :9411

volumes:
  prometheus_data:
  grafana_data:
```

**prometheus.yml:**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'sinatra-mini'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app1:8080', 'app2:8081', 'app3:8082']
  
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

**Acessar dashboards:**
```bash
# Prometheus
open http://localhost:9090

# Grafana (user: admin, pass: GRAFANA_PASSWORD)
open http://localhost:3000

# Jaeger
open http://localhost:16686
```

---

## 5. CI/CD Pipeline (GitHub Actions)

**Preparação futura para v1.2.0**

**.github/workflows/ci.yml:**
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop, main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: sinatra_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      
      - name: Build with Maven
        run: ./mvnw clean install
      
      - name: Run tests
        run: ./mvnw test -Dspring.profiles.active=test-postgres
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
```

---

## 6. Próximos Passos

### Sprint 1 (Semana 1-2 de Fev/2026)

**DevOps Agent - Story 1.1.1 (PostgreSQL Setup)**

1. Criar branch:
   ```bash
   gh issue view 16
   git checkout develop
   git pull origin develop
   git checkout -b feature/1-postgresql-setup
   ```

2. Implementar artefatos:
   - `docker-compose.yml`
   - `application-dev.yml`
   - `application-prod.yml`
   - `.env.example`

3. Testar:
   ```bash
   docker-compose up -d
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. Criar PR:
   ```bash
   git add .
   git commit -m "feat: PostgreSQL setup #16"
   git push origin feature/1-postgresql-setup
   gh pr create --title "feat: PostgreSQL setup #16" --body "Closes #16"
   ```

### Sprint 2 (Semana 3-4 de Fev/2026)

**DevOps Agent - Story 1.1.4 (Backup)**

- Implementar scripts de backup
- Configurar cron jobs
- Testar restore
- Documentar RTO/RPO

---

## 7. Referências

- [ROADMAP-ARQUITETURA.md](../../documentacao/evolucao-arquitetura/ROADMAP-ARQUITETURA.md) - Roadmap completo
- [agile-agents.md](./agile-agents.md) - Backlog e priorização
- [engineer-agents.md](./engineer-agents.md) - Padrões de código
- [Docker Documentation](https://docs.docker.com/) - Referência de containers
- [PostgreSQL Documentation](https://www.postgresql.org/docs/) - Referência de banco de dados