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

## 🎯 Épico 1: Migração para PostgreSQL (v1.1.0)

**Objetivo:** Sair do H2 in-memory para um banco de dados de produção com persistência real.

**Prioridade:** 🔴 CRÍTICA  
**Estimativa:** 2 sprints (3 semanas)  
**Status:** ⏳ PLANEJADO

### Stories

#### Story 1.1.1: Setup PostgreSQL e Configuração de Ambientes

**Como** DevOps  
**Quero** configurar PostgreSQL em desenvolvimento, staging e produção  
**Para** ter ambientes isolados e consistentes

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

**Como** Engineer  
**Quero** versionamento de schema com migrations  
**Para** ter histórico auditável e deployments reproduzíveis

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

**Como** QA  
**Quero** validar que todas as queries funcionam no PostgreSQL  
**Para** garantir paridade com H2

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

**Como** SRE  
**Quero** estratégia automatizada de backup  
**Para** recuperar dados em caso de falha

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

**Objetivo:** Implementar cache em Redis para reduzir carga no banco e melhorar latência.

**Prioridade:** 🟡 ALTA  
**Estimativa:** 1 sprint (2 semanas)  
**Status:** ⏳ PLANEJADO

### Stories

#### Story 1.1.5: Implementação de Cache L1 (Caffeine)

**Como** Engineer  
**Quero** cache local na JVM  
**Para** reduzir latência de dados frequentes (< 1ms)

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

**Como** Engineer  
**Quero** cache distribuído para clusters  
**Para** compartilhar cache entre múltiplas instâncias da aplicação

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

**Objetivo:** Eliminar single point of failure com load balancer e réplicas.

**Prioridade:** 🟢 MÉDIA  
**Estimativa:** 2 sprints (3 semanas)  
**Status:** 📋 BACKLOG

### Stories

#### Story 1.2.1: Load Balancer e Múltiplas Instâncias

**Como** SRE  
**Quero** load balancer na frente de N instâncias  
**Para** distribuir tráfego e ter zero downtime

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

**Como** DBA  
**Quero** read replicas para distribuir queries de leitura  
**Para** desafogar a primary e melhorar latência de relatórios

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

**Objetivo:** Visibilidade completa do sistema com métricas, logs e tracing.

**Prioridade:** 🟡 ALTA  
**Estimativa:** 1 sprint (2 semanas)  
**Status:** 📋 BACKLOG

### Stories

#### Story 1.2.3: Métricas com Prometheus + Grafana

**Como** SRE  
**Quero** dashboards de métricas em tempo real  
**Para** identificar gargalos e anomalias rapidamente

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

**Como** Engineer  
**Quero** rastreamento de requisições distribuídas  
**Para** debugar latência entre serviços

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

**Objetivo:** Escalar para 10.000+ lojas com isolamento de dados.

**Prioridade:** 🟢 MÉDIA  
**Estimativa:** 3 sprints (6 semanas)  
**Status:** 💡 PROPOSTO

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

**Objetivo:** Separar responsabilidades de leitura/escrita para escala extrema.

**Prioridade:** 🔵 BAIXA  
**Estimativa:** 4 sprints (8 semanas)  
**Status:** 💡 PROPOSTO

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

## 📚 Referências e Documentação

- [05-MELHORIAS-ARQUITETURA.md](../docs/05-MELHORIAS-ARQUITETURA.md) - Propostas detalhadas
- [06-ANALISE-TECNICA-DECISOES.md](../docs/06-ANALISE-TECNICA-DECISOES.md) - Trade-offs
- [CHANGELOG.md](CHANGELOG.md) - Histórico de versões
- [QA-TEST-REPORT.md](QA-TEST-REPORT.md) - Baseline de qualidade

---

**Próximo Passo:** Criar branch `feature/epic-1-postgresql` e iniciar Story 1.1.1

**Contato:** Agente Agile  
**Última Atualização:** 21/01/2026
