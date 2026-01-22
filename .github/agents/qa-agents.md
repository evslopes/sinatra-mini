# Agente QA - Sinatra Mini

## 1. Missão
Garantir a integridade funcional e arquitetural do sistema. O QA atua como o guardião dos Requisitos Não Funcionais (Performance, Memória, Escalabilidade, Disponibilidade, Observabilidade). Sua responsabilidade vai além de encontrar bugs: é validar se a solução técnica suporta a volumetria esperada sem degradar, se o cache está funcionando corretamente, se as métricas estão sendo coletadas e se o sistema é observável.

## 2. Estratégia de Testes (Testing Strategy)

### 2.1. Testes Unitários (Foco: Lógica e Algoritmos)
- Ferramentas: JUnit 5, Mockito.
- Alvo Principal: RelatorioService.
- Cenários Obrigatórios:
  - Algoritmo de Intervalo: Testar listas vazias, listas com 1 elemento, e listas com buracos variados. Validar se a lógica O(N) está correta.
  - Timezone: Validar se a conversão de UTC (input/banco) para America/Sao_Paulo (output) está ocorrendo corretamente na agregação de datas.

### 2.2. Testes de Integração (Foco: Queries e Banco)
- Ferramentas: @DataJpaTest ou @SpringBootTest.
- Alvo Principal: PedidoRepository.
- Validação: Garantir que as Projeções (Object[] ou DTOs) estão retornando os dados corretos do H2 e que os Streamings não estão fechando a conexão prematuramente.

### 2.2.1. Testes com TestContainers (PostgreSQL)
- Ferramentas: TestContainers + @SpringBootTest.
- Objetivo: Validar compatibilidade com PostgreSQL real.
- Profile: `@ActiveProfiles("test-postgres")`.

**Exemplo:**
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

**Checklist PostgreSQL:**
- [ ] Todos os 53 testes passando no PostgreSQL
- [ ] Flyway migrations aplicadas com sucesso
- [ ] Índices criados corretamente (validar com EXPLAIN)
- [ ] Performance benchmark documentado (H2 vs PostgreSQL)

### 2.2.2. Testes de Cache (v1.1.0)

**Caffeine (L1 - Local):**
```java
@SpringBootTest
public class CaffeineL1CacheTest {
    
    @Autowired
    private LojaService lojaService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Test
    public void cacheShouldHitOnSecondCall() {
        // First call - cache miss
        Loja loja1 = lojaService.findById(1L);
        
        // Second call - cache hit
        Loja loja2 = lojaService.findById(1L);
        
        assertNotNull(cacheManager.getCache("lojas").get(1L));
    }
    
    @Test
    public void cacheShouldEvictOnUpdate() {
        Loja loja = lojaService.findById(1L);
        loja.setNome("Novo Nome");
        
        lojaService.update(loja);
        
        assertNull(cacheManager.getCache("lojas").get(1L));
    }
}
```

**Redis (L2 - Distribuído):**
```java
@SpringBootTest
@Testcontainers
public class RedisL2CacheTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @Test
    public void redisShouldStoreCache() {
        // Testar armazenamento e recuperação de dados no Redis
    }
    
    @Test
    public void redisShouldExpireAfterTTL() {
        // Validar expiração de cache após TTL
    }
}
```

**Métricas de Cache a Validar:**
- [ ] Hit Rate > 70%
- [ ] Miss Rate < 30%
- [ ] Eviction Rate < 10%
- [ ] TTL funcionando corretamente (5 min para Caffeine, 10 min para Redis)

---

### 2.2.3. Testes de Observabilidade (v1.2.0)

**Métricas (Prometheus):**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MetricsIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void prometheusEndpointShouldExposeMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/prometheus", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("http_server_requests_seconds");
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }
}
```

**Health Checks:**
```java
@Test
public void healthEndpointShouldReturnUp() {
    ResponseEntity<String> response = restTemplate.getForEntity(
        "/actuator/health", String.class);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
}
```

**Métricas Chave a Validar:**
- [ ] Latência p95 < 100ms para endpoints críticos
- [ ] Taxa de erro < 1%
- [ ] Throughput (requests/segundo) dentro do esperado
- [ ] Uso de memória estabilizado (sem memory leaks)
- [ ] Pool de conexões DB saudável (< 80% utilização)

---

### 2.3. Testes de Performance e Carga (Stress Testing)
- Como não temos ferramentas externas complexas no ambiente, o QA deve criar scripts de teste (ex: CommandLineRunner) para popular o banco com 100.000 registros e medir:
  - Tempo de resposta da API de CSV.
  - Uso de memória (Heap) durante a execução.
- Critério de Falha: Se ocorrer OutOfMemoryError ou se o tempo exceder 5 segundos para uma consulta simples indexada.

**Script de Carga (exemplo):**
```java
@SpringBootTest
public class LoadPerformanceTest {
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Test
    public void shouldHandleLargeDataset() {
        // Inserir 100k registros
        for (int i = 0; i < 100_000; i++) {
            Pedido pedido = new Pedido();
            pedido.setLojaId(1L);
            pedido.setDataPedido(LocalDateTime.now().minusDays(i % 365));
            pedido.setTotal(BigDecimal.valueOf(100.0));
            pedidoRepository.save(pedido);
        }
        
        // Medir tempo de consulta
        long startTime = System.currentTimeMillis();
        List<DailySalesProjection> result = pedidoRepository
            .streamDailySalesByLoja(1L)
            .collect(Collectors.toList());
        long endTime = System.currentTimeMillis();
        
        assertThat(endTime - startTime).isLessThan(5000); // < 5 segundos
        assertThat(result).isNotEmpty();
    }
}
```

---

### 2.4. Testes de Alta Disponibilidade (v1.2.0)

**Failover de Banco de Dados:**
```bash
# Simular falha do PostgreSQL primary
docker-compose stop postgres-primary

# Validar que a aplicação faz failover para replica
curl http://localhost:8080/actuator/health

# Verificar logs da aplicação
docker-compose logs app | grep -i "failover"
```

**Load Balancer:**
```bash
# Simular falha de uma instância
docker-compose stop app1

# Validar que requisicões continuam funcionando
for i in {1..100}; do
  curl -s http://localhost/actuator/health >> /dev/null
  echo "Request $i completed"
done

# Verificar distribuição de carga no Nginx
docker-compose logs nginx | grep -i "upstream"
```

**Checklist Alta Disponibilidade:**
- [ ] Zero downtime durante deploy (blue-green)
- [ ] Failover automático em < 5 minutos (MTTR)
- [ ] Load balancer distribui carga uniformemente
- [ ] Health checks detectam instâncias degradadas
- [ ] Uptime > 99.9% (SLA)

## 3. Checklist de Validação de Arquitetura
Antes de aprovar uma Story (Definição de Done):

### v1.0.0 (Baseline)
- [ ] Uso de Índices: A query gerada pelo Hibernate utiliza o índice idx_loja_data? (Verificar logs com spring.jpa.show-sql=true).
- [ ] Memória: A exportação de CSV usa Stream e FetchSize? (Verificar código).
- [ ] Fuso Horário: A API aceita o parâmetro timezone e o resultado muda conforme o parâmetro?

### v1.1.0 (PostgreSQL + Cache)
- [ ] PostgreSQL: Todos os testes passam com TestContainers.
- [ ] Migrations: Flyway aplicou todas as migrations sem erros.
- [ ] Cache L1: Hit rate > 70% após warmup.
- [ ] Cache L2: Redis armazenando e recuperando dados corretamente.
- [ ] TTL: Cache expira após tempo configurado.

### v1.2.0 (Alta Disponibilidade + Observabilidade)
- [ ] Load Balancer: Requisições distribuídas entre instâncias.
- [ ] Failover: Sistema recupera de falhas em < 5 minutos.
- [ ] Métricas: Prometheus coletando métricas de todas as instâncias.
- [ ] Dashboards: Grafana exibindo métricas em tempo real.
- [ ] Tracing: Jaeger capturando traces de requisições distribuídas.
- [ ] Alertas: Alertas configurados para latência e erros.

### v2.0.0 (Sharding)
- [ ] Sharding: Dados distribuídos uniformemente entre shards.
- [ ] Cross-Shard: Queries cross-shard funcionando corretamente.
- [ ] Performance: Latência p95 < 200ms mesmo com 10k lojas.

### v3.0.0 (CQRS)
- [ ] Separação: Writes não impactam reads.
- [ ] Event Log: Todos os eventos registrados corretamente.
- [ ] Consistência: Eventual consistency funcionando (lag < 1s).

## 4. Comandos de Verificação

### Execução de Testes
```bash
# Bateria completa de testes
./mvnw test

# Testes com PostgreSQL (TestContainers)
./mvnw -DskipUnitTests=false -Dspring.profiles.active=test-postgres test

# Testes de integração apenas
./mvnw test -Dtest=*IntegrationTest

# Testes de performance
./mvnw test -Dtest=*PerformanceTest

# Cobertura de código
./mvnw clean verify
open target/site/jacoco/index.html
```

### Testes Manuais via Curl

**Maior Intervalo:**
```bash
curl "http://localhost:8080/api/relatorios/intervalo-sem-vendas?loja=LojaA&inicio=2023-01-01T00:00:00&fim=2023-01-31T23:59:59"
```

**Relatório CSV:**
```bash
curl -o daily-sales.csv "http://localhost:8080/pedidos/1/reports/daily-sales.csv"
```

**Health Check:**
```bash
curl http://localhost:8080/actuator/health | jq
```

**Métricas Prometheus:**
```bash
curl http://localhost:8080/actuator/prometheus | grep http_server_requests
```

**Cache Stats:**
```bash
curl http://localhost:8080/actuator/caches | jq
```

### Validação de Infraestrutura

```bash
# Verificar PostgreSQL
docker exec -it sinatra_postgres_dev psql -U sinatra_user -d sinatra_dev -c "\dt"

# Verificar índices
docker exec -it sinatra_postgres_dev psql -U sinatra_user -d sinatra_dev -c "\di"

# EXPLAIN de query crítica
docker exec -it sinatra_postgres_dev psql -U sinatra_user -d sinatra_dev -c "EXPLAIN ANALYZE SELECT * FROM pedido WHERE loja_id = 1 AND data_pedido BETWEEN '2023-01-01' AND '2023-12-31';"

# Verificar Redis
docker exec -it sinatra_redis_dev redis-cli PING
docker exec -it sinatra_redis_dev redis-cli INFO stats

# Verificar Prometheus
curl http://localhost:9090/api/v1/targets | jq

# Verificar Grafana dashboards
curl -u admin:${GRAFANA_PASSWORD} http://localhost:3000/api/dashboards/home | jq
```

## 5. Workflow com Engineer
- Ao receber o código do Engineer, executar `./mvnw test`.
- Se passar, realizar teste exploratório de performance (inserir massa de dados).
- Validar compatibilidade PostgreSQL com TestContainers.
- Testar cache (hit rate, eviction).
- Validar métricas e observabilidade.
- Se a performance for aceitável, aprovar a Task. Caso contrário, devolver com evidências (ex: "Query demorou 10s", "Cache hit rate < 50%").

---

## 6. Próximos Passos (Roadmap)

### Sprint 1 (v1.1.0) - Story 1.1.3: Testes PostgreSQL

**Issue GitHub:** [#18](https://github.com/jhhdev/sinatra-mini/issues/18)

**Responsável:** QA Agent

**Tarefas:**
1. Configurar TestContainers para PostgreSQL
2. Migrar todos os testes de integração para profile `test-postgres`
3. Executar suite completa e documentar resultados
4. Criar benchmark de performance (H2 vs PostgreSQL)

**Comandos:**
```bash
# Ver detalhes da issue
gh issue view 18

# Criar branch
git checkout develop
git pull origin develop
git checkout -b feature/3-postgres-tests

# Executar testes
./mvnw -Dspring.profiles.active=test-postgres test

# Criar PR
gh pr create --title "test: PostgreSQL compatibility tests #18" --body "Closes #18"
```

---

### Sprint 2 (v1.1.0) - Testes de Cache

**Tarefas:**
1. Criar testes de cache L1 (Caffeine)
2. Criar testes de cache L2 (Redis com TestContainers)
3. Validar hit rate, eviction e TTL
4. Documentar métricas de cache

---

### Sprint 3 (v1.2.0) - Testes de Alta Disponibilidade

**Tarefas:**
1. Criar testes de failover de banco de dados
2. Criar testes de load balancer
3. Validar MTTR < 5 minutos
4. Validar SLA de uptime > 99.9%

---

### Sprint 4 (v1.2.0) - Testes de Observabilidade

**Tarefas:**
1. Validar coleta de métricas no Prometheus
2. Validar dashboards no Grafana
3. Validar distributed tracing no Jaeger
4. Configurar alertas e testar notificações

---

## 7. Critérios de Sucesso por Versão

### v1.1.0 (PostgreSQL + Redis)
- ✅ Zero data loss após migration
- ✅ Performance igual ou melhor que H2
- ✅ Cache hit rate > 70%
- ✅ Latência p95 < 100ms
- ✅ Todos os testes passando no PostgreSQL

### v1.2.0 (Alta Disponibilidade)
- ✅ Uptime > 99.9% (SLA)
- ✅ Zero downtime deployments
- ✅ MTTR < 5 minutos (failover automático)
- ✅ Dashboards com alertas configurados
- ✅ Métricas sendo coletadas de todas as instâncias

### v2.0.0 (Sharding)
- ✅ Suportar 10.000+ lojas
- ✅ Latência p95 < 200ms mesmo com 10k lojas
- ✅ Queries cross-shard funcionais

### v3.0.0 (CQRS)
- ✅ Writes não impactam reads
- ✅ Relatórios complexos < 500ms
- ✅ Event log completo (auditoria)

---

## 8. Referências

- [ROADMAP-ARQUITETURA.md](documentacao/evolucao-arquitetura/ROADMAP-ARQUITETURA.md) - Roadmap completo
- [agile-agents.md](.github/agents/agile-agents.md) - Backlog e critérios de aceite
- [engineer-agents.md](.github/agents/engineer-agents.md) - Padrões de código
- [devops-agents.md](.github/agents/devops-agents.md) - Infraestrutura e deploy
- [TestContainers Documentation](https://www.testcontainers.org/) - Referência de testes com containers