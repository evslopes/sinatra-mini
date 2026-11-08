# Roadmap de Evolução Arquitetural - Sinatra Mini

**Versão Atual:** v1.0.0 (MVP - Monolito + H2)  
**Data:** 21/01/2026  
**Responsável:** Agente Agile + Engineer

---

## 📊 Visão Geral da Evolução

```
v1.0.0           v1.1.0          v1.2.0          v2.0.0          v3.0.0
MVP            Produção       Alta Disp.     Multi-Tenant    Distribuído
(H2)           (PostgreSQL)   (Replicas)     (Sharding)      (CQRS+Events)
  │                │              │               │               │
  │                │              │               │               │
  ▼                ▼              ▼               ▼               ▼
Atual          Q2 2026        Q3 2026         Q4 2026         Q1 2027
```

---

## 🔗 Quick Links - Issues GitHub

### Épicos
- [#15 - Épico 1: PostgreSQL](https://github.com/evslopes/sinatra-mini/issues/15) 🔴 CRÍTICA
- [#23/#24 - Épico 2: Cache](https://github.com/evslopes/sinatra-mini/issues/23) 🟡 ALTA
- [#26 - Épico 3: Alta Disponibilidade](https://github.com/evslopes/sinatra-mini/issues/26) 🟢 MÉDIA
- [#30 - Épico 4: Observabilidade](https://github.com/evslopes/sinatra-mini/issues/30) 🟡 ALTA
- [#34 - Épico 5: Sharding](https://github.com/evslopes/sinatra-mini/issues/34) 💡 PROPOSTO
- [#39 - Épico 6: CQRS](https://github.com/evslopes/sinatra-mini/issues/39) 💡 PROPOSTO

### Stories em Progresso (v1.1.0)
| Story | Issue | Status | Responsável |
|-------|-------|--------|-------------|
| 1.1.1 - PostgreSQL Setup | [#16](https://github.com/evslopes/sinatra-mini/issues/16) | ⏳ TODO | DevOps |
| 1.1.2 - Flyway | [#17](https://github.com/evslopes/sinatra-mini/issues/17) | ⏳ TODO | Engineer |
| 1.1.3 - Testes | [#18](https://github.com/evslopes/sinatra-mini/issues/18) | ⏳ TODO | QA |
| 1.1.4 - Backup | [#19](https://github.com/evslopes/sinatra-mini/issues/19) | ⏳ TODO | DevOps |
| 1.1.5 - Caffeine | [#20](https://github.com/evslopes/sinatra-mini/issues/20) | ⏳ TODO | Engineer |
| 1.1.6 - Redis | [#21](https://github.com/evslopes/sinatra-mini/issues/21) | ⏳ TODO | Engineer |

**Comandos úteis:**
```bash
# Ver todas as issues abertas
gh issue list --state open

# Filtrar por épico
gh issue list --label "epic:postgresql"

# Atribuir issue ao agente
gh issue edit 16 --add-assignee @me

# Ver detalhes de uma issue
gh issue view 16
```

---

## 🤖 Agentes e Responsabilidades

Cada épico/story será executado por agentes especializados do projeto:

| Agente | Papel | Arquivo | Responsabilidades |
|--------|-------|---------|-------------------|
| **Agile** | Product Owner | [agile-agents.md](../../.github/agents/agile-agents.md) | Backlog, priorização, stories, critérios de aceite |
| **Engineer** | Developer | [engineer-agents.md](../../.github/agents/engineer-agents.md) | Código, arquitetura, padrões, performance |
| **DevOps** | SRE/Infra | [devops-agents.md](../../.github/agents/devops-agents.md) | CI/CD, infraestrutura, deploy, monitoramento |
| **QA** | Quality Assurance | [qa-agents.md](../../.github/agents/qa-agents.md) | Testes, validação, performance, carga |

### Workflow de Execução

```
┌──────────┐
│  Agile   │  1. Cria Story com critérios de aceite
└────┬─────┘
     │
     ▼
┌──────────┐
│ Engineer │  2. Implementa código seguindo padrões
└────┬─────┘
     │
     ▼
┌──────────┐
│  DevOps  │  3. Configura infra e deploy
└────┬─────┘
     │
     ▼
┌──────────┐
│    QA    │  4. Valida funcional + performance
└────┬─────┘
     │
     ▼
   DONE ✅
```

---

## 🎯 Épico 1: Migração para PostgreSQL (v1.1.0)

**Issue GitHub:** [#15](https://github.com/evslopes/sinatra-mini/issues/15)  
**Objetivo:** Sair do H2 in-memory para um banco de dados de produção com persistência real.

**Prioridade:** 🔴 CRÍTICA  
**Estimativa:** 2 sprints (3 semanas)  
**Status:** ⏳ PLANEJADO  
**Agente Lead:** [DevOps](../../.github/agents/devops-agents.md)  
**Agentes Apoio:** [Engineer](../../.github/agents/engineer-agents.md), [QA](../../.github/agents/qa-agents.md)

### Stories

#### Story 1.1.1: Setup PostgreSQL e Configuração de Ambientes

**Issue GitHub:** [#16](https://github.com/evslopes/sinatra-mini/issues/16)  
**Como** DevOps  
**Quero** configurar PostgreSQL em desenvolvimento, staging e produção  
**Para** ter ambientes isolados e consistentes

**Responsável:** 👤 [DevOps Agent](../../.github/agents/devops-agents.md)  
**Reviewers:** Engineer (validação de conexão) + QA (smoke tests)  
**Dependências:** Nenhuma  
**Branch:** `feature/1-postgresql-setup`

**Critérios de Aceite:**
- [ ] PostgreSQL 15+ instalado em todos os ambientes
- [ ] Database `sinatra_dev`, `sinatra_staging`, `sinatra_prod` criados
- [ ] Usuários e permissões configurados (princípio do menor privilégio)
- [ ] Perfis Spring (`application-dev.yml`, `application-prod.yml`) criados
- [ ] Variáveis de ambiente documentadas (`.env.example`)

**Tarefas Técnicas:**
```yaml
# docker-compose.yml para desenvolvimento
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: sinatra_dev
      POSTGRES_USER: sinatra_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # NUNCA 'update' em prod
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

**Riscos:**
- ⚠️ Diferenças de dialeto SQL entre H2 e PostgreSQL
- ⚠️ Performance de índices pode variar

---

#### Story 1.1.2: Migração de Schema com Flyway

**Issue GitHub:** [#17](https://github.com/evslopes/sinatra-mini/issues/17)  
**Como** Engineer  
**Quero** versionamento de schema com migrations  
**Para** ter histórico auditável e deployments reproduzíveis

**Responsável:** 👤 [Engineer Agent](../../.github/agents/engineer-agents.md)  
**Reviewers:** DevOps (validação de migration) + QA (validação de dados)  
**Dependências:** ⬅️ Story 1.1.1 (#16)  
**Branch:** `feature/2-flyway-migrations`

**Critérios de Aceite:**
- [ ] Flyway configurado no `pom.xml`
- [ ] Migration V1__initial_schema.sql criado (DDL completo)
- [ ] Migration V2__add_indexes.sql criado (índices compostos)
- [ ] Migration V3__seed_data.sql criado (dados de teste)
- [ ] Validação de checksum habilitada
- [ ] Rollback scripts documentados

**Estrutura:**
```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_indexes.sql
├── V3__seed_data.sql
└── README.md
```

**V1__initial_schema.sql:**
```sql
CREATE TABLE loja (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    data_criacao DATE NOT NULL
);

CREATE TABLE cliente (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE
);

CREATE TABLE pedido (
    id BIGSERIAL PRIMARY KEY,
    data_pedido TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    total DECIMAL(15, 2) NOT NULL,
    metodo_pagamento VARCHAR(50),
    loja_id BIGINT NOT NULL REFERENCES loja(id),
    cliente_id BIGINT REFERENCES cliente(id)
);

CREATE TABLE item_pedido (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    produto VARCHAR(255) NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(15, 2) NOT NULL
);
```

**V2__add_indexes.sql:**
```sql
CREATE INDEX idx_pedido_loja_data 
ON pedido(loja_id, data_pedido);

CREATE INDEX idx_pedido_status 
ON pedido(status);

CREATE INDEX idx_item_pedido_pedido_id 
ON item_pedido(pedido_id);
```

---

#### Story 1.1.3: Testes de Compatibilidade PostgreSQL

**Issue GitHub:** [#18](https://github.com/evslopes/sinatra-mini/issues/18)  
**Como** QA  
**Quero** validar que todas as queries funcionam no PostgreSQL  
**Para** garantir paridade com H2

**Responsável:** 👤 [QA Agent](../../.github/agents/qa-agents.md)  
**Reviewers:** Engineer (análise de queries) + DevOps (configuração TestContainers)  
**Dependências:** ⬅️ Story 1.1.2 (#17)  
**Branch:** `feature/3-postgres-tests`

**Critérios de Aceite:**
- [ ] Profile de teste `@ActiveProfiles("postgres")` criado
- [ ] TestContainers configurado para testes de integração
- [ ] Todos os 53 testes passando no PostgreSQL
- [ ] Performance benchmark documentado (H2 vs PostgreSQL)

**TestContainers Setup:**
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test-postgres")
public class PostgresIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("sinatra_test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    public void allQueriesShouldWorkOnPostgres() {
        // Executar suite completa de testes
    }
}
```

---

#### Story 1.1.4: Backup e Recovery

**Issue GitHub:** [#19](https://github.com/evslopes/sinatra-mini/issues/19)  
**Como** SRE  
**Quero** estratégia automatizada de backup  
**Para** recuperar dados em caso de falha

**Responsável:** 👤 [DevOps Agent](../../.github/agents/devops-agents.md)  
**Reviewers:** QA (testes de restore) + Agile (validação de RTO/RPO)  
**Dependências:** ⬅️ Story 1.1.1 (#16)  
**Branch:** `feature/4-backup-strategy`

**Critérios de Aceite:**
- [ ] Backup diário automatizado (pg_dump)
- [ ] Retenção: 7 diários, 4 semanais, 3 mensais
- [ ] Script de restore testado
- [ ] Documentação de RTO/RPO (Recovery Time/Point Objective)
- [ ] Backup armazenado em S3/Azure Blob

**Exemplo Cron (Linux):**
```bash
#!/bin/bash
# /etc/cron.daily/postgres-backup.sh

BACKUP_DIR="/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="sinatra_prod"

pg_dump -U sinatra_user -h localhost $DB_NAME | gzip > \
    $BACKUP_DIR/sinatra_backup_$DATE.sql.gz

# Upload para S3
aws s3 cp $BACKUP_DIR/sinatra_backup_$DATE.sql.gz \
    s3://sinatra-backups/postgres/

# Limpar backups antigos (> 7 dias)
find $BACKUP_DIR -type f -mtime +7 -delete
```

**RTO/RPO Target:**
- **RPO (Recovery Point Objective):** 24 horas (backup diário)
- **RTO (Recovery Time Objective):** 2 horas (tempo para restore)

---

## 🎯 Épico 2: Cache Distribuído (v1.1.0)

**Issue GitHub:** [#23](https://github.com/evslopes/sinatra-mini/issues/23) / [#24](https://github.com/evslopes/sinatra-mini/issues/24)  
**Objetivo:** Implementar cache em Redis para reduzir carga no banco e melhorar latência.

**Prioridade:** 🟡 ALTA  
**Estimativa:** 1 sprint (2 semanas)  
**Status:** ⏳ PLANEJADO  
**Agente Lead:** [Engineer](../../.github/agents/engineer-agents.md)  
**Agentes Apoio:** [DevOps](../../.github/agents/devops-agents.md), [QA](../../.github/agents/qa-agents.md)

### Stories

#### Story 1.1.5: Implementação de Cache L1 (Caffeine)

**Issue GitHub:** [#20](https://github.com/evslopes/sinatra-mini/issues/20)  
**Como** Engineer  
**Quero** cache local na JVM  
**Para** reduzir latência de dados frequentes (< 1ms)

**Responsável:** 👤 [Engineer Agent](../../.github/agents/engineer-agents.md)  
**Reviewers:** QA (testes de hit rate) + DevOps (métricas)  
**Dependências:** Nenhuma (pode rodar em paralelo com PostgreSQL)  
**Branch:** `feature/5-caffeine-cache`

**Critérios de Aceite:**
- [ ] Caffeine dependency adicionada
- [ ] Cache para Loja (raramente muda)
- [ ] TTL: 5 minutos
- [ ] Max size: 1000 entradas
- [ ] Métricas expostas (hit rate)

**Configuração:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("lojas");
    }
    
    @Bean
    public Caffeine caffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats();
    }
}

@Service
public class LojaService {
    
    @Cacheable(value = "lojas", key = "#id")
    public Loja findById(Long id) {
        return lojaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Loja não encontrada"));
    }
    
    @CacheEvict(value = "lojas", key = "#loja.id")
    public Loja update(Loja loja) {
        return lojaRepository.save(loja);
    }
}
```

---

#### Story 1.1.6: Implementação de Cache L2 (Redis)

**Issue GitHub:** [#21](https://github.com/evslopes/sinatra-mini/issues/21)  
**Como** Engineer  
**Quero** cache distribuído para clusters  
**Para** compartilhar cache entre múltiplas instâncias da aplicação

**Responsável:** 👤 [Engineer Agent](../../.github/agents/engineer-agents.md)  
**Apoio:** [DevOps Agent](../../.github/agents/devops-agents.md) (setup Redis)  
**Reviewers:** QA (testes de cache distribuído)  
**Dependências:** ⬅️ Story 1.1.5 (#20)  
**Branch:** `feature/6-redis-cache`

**Critérios de Aceite:**
- [ ] Redis configurado (standalone ou cluster)
- [ ] Spring Data Redis integrado
- [ ] Cache de relatórios CSV (TTL: 10 min)
- [ ] Cache de estatísticas (TTL: 1 hora)
- [ ] Serialização otimizada (JSON vs Java Serialization)

**Configuração:**
```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // JSON serialization (mais eficiente que Java Serialization)
        GenericJackson2JsonRedisSerializer serializer = 
            new GenericJackson2JsonRedisSerializer();
        
        template.setDefaultSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        
        return template;
    }
}

@Service
public class PedidoService {
    
    @Autowired
    private RedisTemplate<String, Object> redis;
    
    public List<DailySalesProjection> getDailySales(Long lojaId) {
        String cacheKey = "daily-sales:" + lojaId;
        
        // Tentar buscar do cache
        List<DailySalesProjection> cached = 
            (List) redis.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        // Cache miss - buscar do banco
        List<DailySalesProjection> result = 
            pedidoRepository.streamDailySalesByLoja(lojaId)
                .collect(Collectors.toList());
        
        // Armazenar no cache (TTL: 10 min)
        redis.opsForValue().set(cacheKey, result, 10, TimeUnit.MINUTES);
        
        return result;
    }
}
```

---

## 🎯 Épico 3: Alta Disponibilidade (v1.2.0)

**Issue GitHub:** [#26](https://github.com/evslopes/sinatra-mini/issues/26)  
**Objetivo:** Eliminar single point of failure com load balancer e réplicas.

**Prioridade:** 🟢 MÉDIA  
**Estimativa:** 2 sprints (3 semanas)  
**Status:** 📋 BACKLOG  
**Agente Lead:** [DevOps](../../.github/agents/devops-agents.md)  
**Agentes Apoio:** [Engineer](../../.github/agents/engineer-agents.md), [QA](../../.github/agents/qa-agents.md)

### Stories

#### Story 1.2.1: Load Balancer e Múltiplas Instâncias

**Issue GitHub:** [#27](https://github.com/evslopes/sinatra-mini/issues/27)  
**Como** SRE  
**Quero** load balancer na frente de N instâncias  
**Para** distribuir tráfego e ter zero downtime

**Responsável:** 👤 [DevOps Agent](../../.github/agents/devops-agents.md)  
**Reviewers:** Engineer (validação stateless) + QA (testes de carga)  
**Dependências:** ⬅️ Story 1.1.6 (#21)  
**Branch:** `feature/7-load-balancer`

**Arquitetura:**
```
                     ┌──> [App Instance 1:8080]
[Nginx/ALB] ────────┼──> [App Instance 2:8081]
                     └──> [App Instance 3:8082]
```

**Nginx Config:**
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
        
        # Health check
        proxy_next_upstream error timeout http_500;
    }
}
```

---

#### Story 1.2.2: PostgreSQL Read Replicas

**Issue GitHub:** [#28](https://github.com/evslopes/sinatra-mini/issues/28)  
**Como** DBA  
**Quero** read replicas para distribuir queries de leitura  
**Para** desafogar a primary e melhorar latência de relatórios

**Responsável:** 👤 [DevOps Agent](../../.github/agents/devops-agents.md)  
**Apoio:** [Engineer Agent](../../.github/agents/engineer-agents.md) (routing datasource)  
**Reviewers:** QA (validação de lag e consistência)  
**Dependências:** ⬅️ Story 1.1.1 (#16)  
**Branch:** `feature/8-read-replicas`

**Critérios de Aceite:**
- [ ] 2 read replicas configuradas (streaming replication)
- [ ] Routing: writes → primary, reads → replicas
- [ ] Lag monitoring (< 1 segundo)
- [ ] Failover automático (Patroni/pgpool)

**Spring Data Routing:**
```java
@Configuration
public class DataSourceRoutingConfig {
    
    @Bean
    public DataSource routingDataSource() {
        RoutingDataSource routing = new RoutingDataSource();
        
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("primary", primaryDataSource());
        dataSources.put("replica1", replicaDataSource1());
        dataSources.put("replica2", replicaDataSource2());
        
        routing.setTargetDataSources(dataSources);
        routing.setDefaultTargetDataSource(primaryDataSource());
        
        return routing;
    }
}

@Service
public class PedidoService {
    
    @Transactional(readOnly = false)  // Vai para PRIMARY
    public Pedido save(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }
    
    @Transactional(readOnly = true)  // Vai para REPLICA
    public List<Pedido> findAll() {
        return pedidoRepository.findAll();
    }
}
```

---

## 🎯 Épico 4: Observabilidade e Monitoramento (v1.2.0)

**Issue GitHub:** [#30](https://github.com/evslopes/sinatra-mini/issues/30)  
**Objetivo:** Visibilidade completa do sistema com métricas, logs e tracing.

**Prioridade:** 🟡 ALTA  
**Estimativa:** 1 sprint (2 semanas)  
**Status:** 📋 BACKLOG  
**Agente Lead:** [DevOps](../../.github/agents/devops-agents.md)  
**Agentes Apoio:** [Engineer](../../.github/agents/engineer-agents.md)

### Stories

#### Story 1.2.3: Métricas com Prometheus + Grafana

**Issue GitHub:** [#31](https://github.com/evslopes/sinatra-mini/issues/31)  
**Como** SRE  
**Quero** dashboards de métricas em tempo real  
**Para** identificar gargalos e anomalias rapidamente

**Responsável:** 👤 [DevOps Agent](../../.github/agents/devops-agents.md)  
**Apoio:** [Engineer Agent](../../.github/agents/engineer-agents.md) (instrumentação de código)  
**Reviewers:** QA (validação de métricas)  
**Dependências:** Nenhuma (pode rodar em paralelo)  
**Branch:** `feature/9-observability`

**Métricas Chave:**
- **Latência**: p50, p95, p99 de cada endpoint
- **Throughput**: Requests/segundo
- **Erros**: Taxa de erro 4xx/5xx
- **Recursos**: CPU, memória, conexões DB
- **Cache**: Hit rate, eviction rate

**Configuração:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```java
@Configuration
public class MetricsConfig {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "sinatra-mini")
            .commonTags("environment", "production");
    }
}

// Endpoint de métricas
// GET /actuator/prometheus
```

**Grafana Dashboard:**
- Panel 1: Request Rate (qps)
- Panel 2: Error Rate (%)
- Panel 3: Latency Percentiles (ms)
- Panel 4: Database Connection Pool
- Panel 5: Cache Hit Rate

---

#### Story 1.2.4: Distributed Tracing (OpenTelemetry)

**Issue GitHub:** [#32](https://github.com/evslopes/sinatra-mini/issues/32)  
**Como** Engineer  
**Quero** rastreamento de requisições distribuídas  
**Para** debugar latência entre serviços

**Responsável:** 👤 [Engineer Agent](../../.github/agents/engineer-agents.md)  
**Apoio:** [DevOps Agent](../../.github/agents/devops-agents.md) (setup Jaeger/Tempo)  
**Reviewers:** QA (validação de traces)  
**Dependências:** ⬅️ Story 1.2.3 (#31)  
**Branch:** `feature/10-distributed-tracing`

**Critérios de Aceite:**
- [ ] OpenTelemetry Agent configurado
- [ ] Spans automáticos para HTTP, JDBC, Redis
- [ ] Trace ID propagado via headers
- [ ] Jaeger/Tempo como backend

**Exemplo de Trace:**
```
Trace ID: abc123
├─ [200ms] GET /pedidos/1/reports/daily-sales.csv
   ├─ [5ms]  Cache lookup (Redis) - MISS
   ├─ [180ms] Database query (PostgreSQL)
   │  ├─ [150ms] SELECT pedido WHERE loja_id = 1
   │  └─ [30ms]  Fetch results
   └─ [15ms] CSV generation
```

---

## 🎯 Épico 5: Multi-Tenancy com Sharding (v2.0.0)

**Issue GitHub:** [#34](https://github.com/evslopes/sinatra-mini/issues/34)  
**Objetivo:** Escalar para 10.000+ lojas com isolamento de dados.

**Prioridade:** 🟢 MÉDIA  
**Estimativa:** 3 sprints (6 semanas)  
**Status:** 💡 PROPOSTO  
**Agente Lead:** [Engineer](../../.github/agents/engineer-agents.md)  
**Agentes Apoio:** [DevOps](../../.github/agents/devops-agents.md), [QA](../../.github/agents/qa-agents.md)

### Stories

#### Story 2.0.1: Implementação da Lógica de Sharding

**Issue GitHub:** [#35](https://github.com/evslopes/sinatra-mini/issues/35)  
**Responsável:** 👤 [Engineer Agent](../../.github/agents/engineer-agents.md)  
**Branch:** `feature/11-sharding-logic`

#### Story 2.0.2: Configuração de Múltiplos DataSources

**Issue GitHub:** [#36](https://github.com/evslopes/sinatra-mini/issues/36)  
**Responsável:** 👤 [DevOps Agent](../../.github/agents/devops-agents.md)  
**Dependências:** ⬅️ Story 2.0.1 (#35)  
**Branch:** `feature/12-multi-datasource`

#### Story 2.0.3: Routing de Queries Cross-Shard

**Issue GitHub:** [#37](https://github.com/evslopes/sinatra-mini/issues/37)  
**Responsável:** 👤 [Engineer Agent](../../.github/agents/engineer-agents.md)  
**Dependências:** ⬅️ Story 2.0.2 (#36)  
**Branch:** `feature/13-cross-shard-queries`

### Estratégia de Sharding

**Abordagem:** Hash-based sharding por `loja_id`

```java
public class ShardingService {
    private static final int TOTAL_SHARDS = 4;
    
    public int getShardId(Long lojaId) {
        return (int) (lojaId % TOTAL_SHARDS);
    }
}
```

**Distribuição:**
```
Shard 0: Lojas 0, 4, 8, 12, ... (25%)
Shard 1: Lojas 1, 5, 9, 13, ... (25%)
Shard 2: Lojas 2, 6, 10, 14, ... (25%)
Shard 3: Lojas 3, 7, 11, 15, ... (25%)
```

---

## 🎯 Épico 6: CQRS e Event Sourcing (v3.0.0)

**Issue GitHub:** [#39](https://github.com/evslopes/sinatra-mini/issues/39)  
**Objetivo:** Separar responsabilidades de leitura/escrita para escala extrema.

**Prioridade:** 🔵 BAIXA  
**Estimativa:** 4 sprints (8 semanas)  
**Status:** 💡 PROPOSTO  
**Agente Lead:** [Engineer](../../.github/agents/engineer-agents.md)  
**Agentes Apoio:** [DevOps](../../.github/agents/devops-agents.md)

### Stories

#### Story 3.0.1: Command Service (Write Side)

**Issue GitHub:** [#40](https://github.com/evslopes/sinatra-mini/issues/40)  
**Responsável:** 👤 [Engineer Agent](../../.github/agents/engineer-agents.md)  
**Branch:** `feature/14-command-service`

#### Story 3.0.2: Event Bus com Apache Kafka

**Issue GitHub:** [#41](https://github.com/evslopes/sinatra-mini/issues/41)  
**Responsável:** 👤 [DevOps Agent](../../.github/agents/devops-agents.md)  
**Dependências:** ⬅️ Story 3.0.1 (#40)  
**Branch:** `feature/15-event-bus`

#### Story 3.0.3: Query Service com Read Models

**Issue GitHub:** [#42](https://github.com/evslopes/sinatra-mini/issues/42)  
**Responsável:** 👤 [Engineer Agent](../../.github/agents/engineer-agents.md)  
**Dependências:** ⬅️ Story 3.0.2 (#41)  
**Branch:** `feature/16-query-service`

#### Story 3.0.4: Event Store e Auditoria

**Issue GitHub:** [#43](https://github.com/evslopes/sinatra-mini/issues/43)  
**Responsável:** 👤 [Engineer Agent](../../.github/agents/engineer-agents.md)  
**Dependências:** ⬅️ Story 3.0.2 (#41)  
**Branch:** `feature/17-event-store`

**Arquitetura:**
```
[Write API] → [Command Service] → [PostgreSQL (Commands)]
                      ↓
                 [Event Bus (Kafka)]
                      ↓
[Read API]  ← [Query Service]  ← [Elasticsearch/ClickHouse (Queries)]
```

**Quando implementar:**
- Quando writes impactarem latência de reads
- Quando relatórios complexos travarem o banco
- Quando precisar de auditoria completa (event log)

---

## ✅ Definition of Done (DoD)

Para considerar uma Story como DONE, os seguintes critérios devem ser atendidos:

### Código
- [ ] **Compilação**: `./mvnw clean install` executado com sucesso
- [ ] **Testes**: Todos os testes (unitários + integração) passando
- [ ] **Cobertura**: Mínimo 80% de code coverage para novos códigos
- [ ] **Padrões**: Código segue [copilot-instructions.md](../../.github/copilot-instructions.md)
- [ ] **Review**: Aprovação de pelo menos 1 reviewer (Engineer ou Senior)

### Performance
- [ ] **Índices**: Queries otimizadas com índices compostos
- [ ] **Memória**: Sem memory leaks (validado com profiler)
- [ ] **Latência**: p95 < 100ms para endpoints críticos
- [ ] **Streaming**: Uso de `Stream<T>` para dados > 1000 registros

### DevOps
- [ ] **Branch**: Merged para `develop` via Pull Request
- [ ] **CI/CD**: Pipeline verde no GitHub Actions
- [ ] **Deploy**: Deploy em ambiente de staging bem-sucedido
- [ ] **Rollback**: Plano de rollback documentado

### Documentação
- [ ] **README**: Atualizado se houver novos comandos/configs
- [ ] **API**: Swagger/OpenAPI atualizado (se novos endpoints)
- [ ] **Architecture**: Diagramas atualizados (se mudanças estruturais)
- [ ] **CHANGELOG**: Versão e mudanças documentadas

### QA
- [ ] **Funcional**: Todos os critérios de aceite validados
- [ ] **Regressão**: Suite de regressão executada e passando
- [ ] **Performance**: Testes de carga executados (se aplicável)
- [ ] **Security**: Scan de vulnerabilidades executado (OWASP, Snyk)

---

## 📅 Cronograma Proposto

| Versão | Épicos | Timeline | Status |
|--------|--------|----------|--------|
| **v1.1.0** | PostgreSQL + Redis | Q2 2026 (Abr-Jun) | ⏳ Planejado |
| **v1.2.0** | Alta Disponibilidade + Observabilidade | Q3 2026 (Jul-Set) | 📋 Backlog |
| **v2.0.0** | Sharding Multi-Tenant | Q4 2026 (Out-Dez) | 💡 Proposto |
| **v3.0.0** | CQRS + Event Sourcing | Q1 2027 (Jan-Mar) | 💡 Proposto |

---

## 🎯 Critérios de Sucesso por Versão

### v1.1.0 (PostgreSQL + Redis)
- ✅ Zero data loss após migration
- ✅ Performance igual ou melhor que H2
- ✅ Cache hit rate > 70%
- ✅ Latência p95 < 100ms

### v1.2.0 (Alta Disponibilidade)
- ✅ Uptime > 99.9% (SLA)
- ✅ Zero downtime deployments
- ✅ MTTR < 5 minutos (failover automático)
- ✅ Dashboards com alertas configurados

### v2.0.0 (Sharding)
- ✅ Suportar 10.000+ lojas
- ✅ Latência p95 < 200ms mesmo com 10k lojas
- ✅ Queries cross-shard funcionais (relatórios globais)

### v3.0.0 (CQRS)
- ✅ Writes não impactam reads
- ✅ Relatórios complexos < 500ms
- ✅ Event log completo (auditoria)

---

## ⚠️ Riscos e Mitigação

### Épico 1: PostgreSQL Migration

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|----------|
| Diferenças de dialeto SQL | ALTA | MÉDIO | Usar TestContainers em todos os testes de integração |
| Perda de dados na migração | BAIXA | CRÍTICO | Backup completo + dry-run em staging + rollback plan |
| Performance degradada | MÉDIA | ALTO | Benchmark antes/depois + índices validados com EXPLAIN |
| Downtime no deploy | MÉDIA | ALTO | Blue-green deployment + health checks |

**Responsável Mitigação:** [DevOps Agent](../../.github/agents/devops-agents.md)

### Épico 2: Cache Distribuído

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|----------|
| Redis single point of failure | ALTA | CRÍTICO | Redis Sentinel ou Redis Cluster (3+ nodes) |
| Cache invalidation bugs | MÉDIA | MÉDIO | Testes de consistência + TTL conservador (< 10min) |
| Network latency para Redis | BAIXA | MÉDIO | Redis no mesmo datacenter + connection pooling |
| Thundering herd (cache miss) | MÉDIA | ALTO | Cache warming + staggered TTL |

**Responsável Mitigação:** [Engineer Agent](../../.github/agents/engineer-agents.md)

### Épico 3: Alta Disponibilidade

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|----------|
| Configuração errada de load balancer | MÉDIA | CRÍTICO | Smoke tests + health checks com alertas |
| Replica lag > 5 segundos | BAIXA | ALTO | Monitoring de lag + alertas + fallback para primary |
| Split-brain em failover | BAIXA | CRÍTICO | Quorum-based consensus (Patroni) |
| Sessões perdidas no deploy | ALTA | MÉDIO | Stateless app + Redis para sessões |

**Responsável Mitigação:** [DevOps Agent](../../.github/agents/devops-agents.md) + [QA Agent](../../.github/agents/qa-agents.md)

### Épico 4: Observabilidade

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|----------|
| Overhead de tracing (> 5%) | MÉDIA | MÉDIO | Sampling rate 1% em produção (100% em staging) |
| Alertas falsos positivos | ALTA | BAIXO | Tuning de thresholds + runbook documentado |
| Custo de armazenamento de métricas | BAIXA | MÉDIO | Retenção de 15 dias + agregação horária após 3 dias |

**Responsável Mitigação:** [DevOps Agent](../../.github/agents/devops-agents.md)

---

## 📚 Referências e Documentação

- [05-MELHORIAS-ARQUITETURA.md](../../docs/story-1.1-indices-compostos.md) - Propostas detalhadas
- [06-ANALISE-TECNICA-DECISOES.md](../../README.md) - Trade-offs
- [CHANGELOG.md](../../CHANGELOG.md) - Histórico de versões
- [QA-TEST-REPORT.md](../../docs/QA-TEST-REPORT.md) - Baseline de qualidade

---

## 🚀 Próximos Passos Imediatos

### Sprint 1 (Semana 1-2 de Fev/2026)

1. **[Agile Agent](../../.github/agents/agile-agents.md)**: ✅ Issues criadas no GitHub
   - [#15](https://github.com/evslopes/sinatra-mini/issues/15): Épico 1 - PostgreSQL
   - [#16](https://github.com/evslopes/sinatra-mini/issues/16): Story 1.1.1 - Setup PostgreSQL
   - [#17](https://github.com/evslopes/sinatra-mini/issues/17): Story 1.1.2 - Flyway
   - [#18](https://github.com/evslopes/sinatra-mini/issues/18): Story 1.1.3 - Testes
   - [#19](https://github.com/evslopes/sinatra-mini/issues/19): Story 1.1.4 - Backup

2. **[DevOps Agent](../../.github/agents/devops-agents.md)**: Iniciar Story 1.1.1 (#16)
   ```bash
   gh issue view 16  # Ver detalhes da issue
   git checkout develop
   git pull origin develop
   git checkout -b feature/1-postgresql-setup
   ```

3. **[Engineer Agent](../../.github/agents/engineer-agents.md)**: Revisar dependências e preparar Flyway migrations

4. **[QA Agent](../../.github/agents/qa-agents.md)**: Preparar ambiente de testes com TestContainers

### Sprint 2 (Semana 3-4 de Fev/2026)

1. **[Engineer Agent](../../.github/agents/engineer-agents.md)**: Implementar Stories 1.1.2 (Flyway)
2. **[QA Agent](../../.github/agents/qa-agents.md)**: Executar Story 1.1.3 (Testes)
3. **[DevOps Agent](../../.github/agents/devops-agents.md)**: Implementar Story 1.1.4 (Backup)
4. **[Agile Agent](../../.github/agents/agile-agents.md)**: Sprint Review + Planning do Épico 2 (Cache)

---

## 📞 Contatos e Responsáveis

| Papel | Agente | Responsabilidade |
|-------|--------|------------------|
| **Product Owner** | [Agile Agent](../../.github/agents/agile-agents.md) | Priorização, backlog, critérios de aceite |
| **Tech Lead** | [Engineer Agent](../../.github/agents/engineer-agents.md) | Arquitetura, código, reviews técnicos |
| **SRE/DevOps** | [DevOps Agent](../../.github/agents/devops-agents.md) | Infraestrutura, deploy, monitoramento |
| **Quality Lead** | [QA Agent](../../.github/agents/qa-agents.md) | Testes, validação, performance |

---

**Versão do Roadmap:** 1.1  
**Última Atualização:** 21/01/2026  
**Próxima Revisão:** 01/03/2026 (após conclusão Épico 1)
