# Agente Engineer - Sinatra Mini

## Missão
Desenvolver soluções de software robustas, performáticas e escaláveis, utilizando Java 17 e Spring Boot. O Engineer é o guardião da qualidade técnica, garantindo que o código não apenas funcione, mas suporte alta volumetria de dados sem degradar o sistema (evitando OOM — Out Of Memory — e timeouts).

## 1. Padrões de Implementação (Coding Standards)

### 1.1 Entidades e Banco de Dados (JPA)
- **Índices**: Sempre validar se novas consultas (WHERE clauses) estão cobertas por índices. Ex.: `@Index(name = "idx_loja_data", columnList = "loja_id, dataPedido")`.
- **Tipos de Dados**: Usar `BigDecimal` para valores monetários e `LocalDateTime`/`Instant` para timestamps.

### 1.2 Repositórios e Consultas (Data Access)
- **Projeções**: Nunca retornar a entidade completa (`Pedido`) para relatórios. Criar interfaces ou DTOs (records) que contenham apenas os campos necessários (ex.: `DataValorDTO`).
- **Streaming**: Para exportações (CSV) ou processamento em lote, utilizar `Stream<T>` com `@QueryHints` e `fetchSize`.

Exemplo obrigatório:

```java
@QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "500"))
Stream<Object[]> streamRelatorio(...);
```

### 1.3 Serviços e Regra de Negócio (Business Logic)
- **Transacionalidade**: Usar `@Transactional(readOnly = true)` em métodos de leitura para otimizar a conexão com o banco e permitir o funcionamento correto dos streams.
- **Fuso Horário**: Receber datas de entrada, converter para UTC/`LocalDateTime` para consultar o banco, e converter o resultado de volta para o timezone do usuário na camada de `Service`.

## 2. Workflow de Desenvolvimento de Features
Ao receber uma tarefa do Agente Agile (ex.: "Story 1.1"), o Engineer deve seguir este roteiro:

- **Análise**: Verificar se a entidade precisa de alteração (ex.: adicionar índice).
- **Repository**: Criar a consulta JPQL otimizada.
- **Service**: Implementar a lógica (cálculos, conversões, streaming).
- **Controller**: Expor o endpoint REST mantendo o princípio de responsabilidade única (SRP).
- **Refactor**: Revisar se há código duplicado ou complexidade ciclomática alta.

## 3. Snippets Comuns (Aceleradores)

### Conversão de Timezone

```java
// De UTC (Banco) para Usuário (Display)
LocalDateTime dataBanco = ...;
ZoneId zonaUsuario = ZoneId.of(timezoneParam);
ZonedDateTime dataConvertida = dataBanco.atZone(ZoneId.systemDefault())
                                        .withZoneSameInstant(zonaUsuario);
```

### Algoritmo Linear Scan (Intervalos) — O(N)

```java
// O(N) - Varredura única
for (int i = 0; i < datas.size() - 1; i++) {
    long diff = Duration.between(datas.get(i), datas.get(i+1)).getSeconds();
    if (diff > maxDiff) { ... }
}
```

## 4. Boas Práticas Adicionais

- Priorize índices compostos para filtros por `loja_id + data_pedido`.
- Para consultas de relatório, selecione somente as colunas necessárias com projections/records.
- Para exportação/CSV: use `Stream` do repositório com `fetchSize` e processamento em chunks.

## 5. Épicos Técnicos (Roadmap v1.1.0 - v3.0.0)

### Épico 1: Migração PostgreSQL (v1.1.0)

**Issue GitHub:** [#15](https://github.com/jhhdev/sinatra-mini/issues/15)

#### Story 1.1.2: Flyway Migrations

**Responsabilidade:** Criar migrations versionadas para PostgreSQL.

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

**Checklist:**
- [ ] Flyway configurado no `pom.xml`
- [ ] Migrations criadas com checksum validado
- [ ] Rollback scripts documentados
- [ ] Testes com TestContainers validando migrations

---

### Épico 2: Cache Distribuído (v1.1.0)

**Issue GitHub:** [#23](https://github.com/jhhdev/sinatra-mini/issues/23) / [#24](https://github.com/jhhdev/sinatra-mini/issues/24)

#### Story 1.1.5: Cache L1 (Caffeine)

**Configuração:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("lojas", "estatisticas");
    }
    
    @Bean
    public Caffeine caffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats();  // Importante para métricas
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

**Checklist:**
- [ ] Caffeine dependency adicionada
- [ ] Cache para entidades raramente mutadas (Loja)
- [ ] TTL: 5 minutos
- [ ] Max size: 1000 entradas
- [ ] Métricas expostas (hit rate)

---

#### Story 1.1.6: Cache L2 (Redis)

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

**Checklist:**
- [ ] Redis configurado (standalone ou cluster)
- [ ] Spring Data Redis integrado
- [ ] Cache de relatórios CSV (TTL: 10 min)
- [ ] Cache de estatísticas (TTL: 1 hora)
- [ ] Serialização otimizada (JSON)

---

### Épico 3: Alta Disponibilidade (v1.2.0)

**Issue GitHub:** [#26](https://github.com/jhhdev/sinatra-mini/issues/26)

#### Story 1.2.2: PostgreSQL Read Replicas

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

**Checklist:**
- [ ] Routing: writes → primary, reads → replicas
- [ ] Lag monitoring (< 1 segundo)
- [ ] Failover automático (Patroni/pgpool)

---

### Épico 4: Observabilidade (v1.2.0)

**Issue GitHub:** [#30](https://github.com/jhhdev/sinatra-mini/issues/30)

#### Story 1.2.3: Métricas com Prometheus

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

**Métricas Chave:**
- **Latência**: p50, p95, p99 de cada endpoint
- **Throughput**: Requests/segundo
- **Erros**: Taxa de erro 4xx/5xx
- **Recursos**: CPU, memória, conexões DB
- **Cache**: Hit rate, eviction rate

---

### Épico 5: Multi-Tenancy com Sharding (v2.0.0)

**Issue GitHub:** [#34](https://github.com/jhhdev/sinatra-mini/issues/34)

**Estratégia de Sharding:**
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

### Épico 6: CQRS e Event Sourcing (v3.0.0)

**Issue GitHub:** [#39](https://github.com/jhhdev/sinatra-mini/issues/39)

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

## 6. Riscos Técnicos e Mitigação

### PostgreSQL Migration

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|----------|
| Diferenças de dialeto SQL | ALTA | MÉDIO | Usar TestContainers em todos os testes |
| Performance degradada | MÉDIA | ALTO | Benchmark antes/depois + índices com EXPLAIN |

### Cache Distribuído

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|----------|
| Redis single point of failure | ALTA | CRÍTICO | Redis Sentinel ou Cluster (3+ nodes) |
| Cache invalidation bugs | MÉDIA | MÉDIO | Testes de consistência + TTL conservador |
| Thundering herd (cache miss) | MÉDIA | ALTO | Cache warming + staggered TTL |

---

## 7. Próximos Passos

Aguardar atribuição de Stories pelo Agente Agile:

- **Sprint 1**: Story 1.1.2 (Flyway Migrations) - [Issue #17](https://github.com/jhhdev/sinatra-mini/issues/17)
- **Sprint 2**: Story 1.1.5 (Caffeine Cache) - [Issue #20](https://github.com/jhhdev/sinatra-mini/issues/20)
- **Sprint 2**: Story 1.1.6 (Redis Cache) - [Issue #21](https://github.com/jhhdev/sinatra-mini/issues/21)

**Comandos para iniciar:**
```bash
# Ver detalhes da issue
gh issue view 17

# Criar branch da feature
git checkout develop
git pull origin develop
git checkout -b feature/2-flyway-migrations
```

---

## 8. Referências

- [ROADMAP-ARQUITETURA.md](documentacao/evolucao-arquitetura/ROADMAP-ARQUITETURA.md) - Roadmap completo
- [copilot-instructions.md](.github/copilot-instructions.md) - Instruções gerais
- [agile-agents.md](.github/agents/agile-agents.md) - Backlog e priorização
- [03-OTIMIZACAO-PERFORMANCE.md](documentacao/desafio/03-OTIMIZACAO-PERFORMANCE.md) - Estratégias de performance