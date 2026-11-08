# Instruções Gerais do Projeto — Sinatra Mini

## 1. Contexto do Projeto
Sinatra Mini é um SaaS simplificado para gestão de vendas de múltiplas lojas. O foco principal continua sendo **desempenho, escalabilidade e eficiência de memória**, com prioridade na migração para ambientes de produção conforme o Roadmap (v1.1.0).

## 2. Stack Tecnológica (atualizado)
- **Linguagem**: Java 17
- **Framework**: Spring Boot 3.2.12
- **Banco (produção)**: PostgreSQL 15+
- **Banco (testes e desenvolvimento leve)**: H2 (in-memory)
- **Migrations**: Flyway (para versionamento de schema)
- **Cache**: Caffeine (L1) + Redis (L2) para ambientes distribuídos
- **Build**: Maven
- **Persistência**: JPA/Hibernate

## 3. Arquitetura em Camadas (Layered Architecture)

### 3.1 Estrutura de Pacotes
```
src/main/java/pro/sinatra/mini/
├── controller/    # REST endpoints (Presentation Layer)
├── service/       # Business logic (Business Layer)
├── repository/    # Data access (Persistence Layer)
├── entity/        # JPA entities (Domain Model)
└── reports/       # DTOs e Projections para relatórios
```

### 3.2 Fluxo de Responsabilidades (RIGOROSO)
- **Controller**: Recebe requisições HTTP → valida parâmetros → delega para Service → retorna ResponseEntity
  - Exemplo: [PedidoController.java](../src/main/java/pro/sinatra/mini/controller/PedidoController.java)
  - ❌ NUNCA fazer lógica de negócio ou conversões de fuso horário
  - ✅ Usar `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` para parâmetros de data

- **Service**: Lógica de negócio → conversões de fuso horário → orquestração de múltiplos repositórios → transações
  - Exemplo: [MaiorIntervaloSemVendasService.java](../src/main/java/pro/sinatra/mini/service/MaiorIntervaloSemVendasService.java)
  - ✅ `@Transactional(readOnly = true)` para queries
  - ✅ Injeção via construtor (padrão do projeto)
  
- **Repository**: Queries otimizadas → Projections → Streaming
  - Exemplo: [PedidoRepository.java](../src/main/java/pro/sinatra/mini/repository/PedidoRepository.java)
  - ✅ Definir interface de Projection DENTRO do Repository: `interface DailySalesProjection { ... }`
  - ✅ Retornar `Stream<T>` para queries de relatório
  - ✅ Preparar queries compatíveis com PostgreSQL (ex.: evitar funções específicas do H2)

- **Entity**: Modelo de dados com índices compostos
  - Exemplo: [Pedido.java](../src/main/java/pro/sinatra/mini/entity/Pedido.java#L18-L20)
  - ✅ `@Index(name = "idx_pedido_loja_data", columnList = "loja_id, data_pedido")`

## 4. Padrões de Performance Críticos

### 4.1 Manipulação de Dados (OBRIGATÓRIO)
❌ **NUNCA** usar `repository.findAll()` em tabelas de alto volume  
✅ **SEMPRE** usar Projections para queries de relatório:
```java
interface DailySalesProjection {
    java.time.LocalDate getDia();
    Double getTotal();
}

Stream<DailySalesProjection> streamDailySalesByLoja(@Param("lojaId") Long lojaId);
```

### 4.2 Exportação CSV/Streaming
✅ Usar `Stream<T>` + `StreamingResponseBody` para evitar OOM:
```java
@GetMapping(value = "/{lojaId}/reports/daily-sales.csv", produces = "text/csv")
public ResponseEntity<StreamingResponseBody> exportCsv(@PathVariable Long lojaId) {
    StreamingResponseBody body = outputStream -> {
        try (Stream<DailySalesProjection> stream = pedidoService.streamDailySalesByLoja(lojaId);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            writer.write("date,total\n");
            stream.forEach(p -> { /* escrever linha */ });
        }
    };
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daily-sales.csv\"")
        .body(body);
}
```

### 4.3 Índices Compostos
✅ Toda query com filtro por `loja_id` + temporal deve ter índice composto:
```java
@Index(name = "idx_pedido_loja_data", columnList = "loja_id, data_pedido")
```

### 4.4 Datas e Fusos Horários
- **Armazenamento**: `LocalDateTime` (sem fuso) no banco
- **Conversões**: APENAS na camada Service usando `ZoneId`
- ❌ NUNCA fazer conversões de fuso na Controller ou Repository

### 4.5 Algoritmos
- ❌ Evitar O(N²) em processamento de listas
- ✅ Preferir varredura linear O(N)
- Exemplo: [MaiorIntervaloSemVendasService.java](../src/main/java/pro/sinatra/mini/service/MaiorIntervaloSemVendasService.java#L86-L98) usa algoritmo O(N) para calcular gaps entre vendas

## 5. Testes (Padrões Estabelecidos)

### 5.1 Estrutura
```
src/test/java/pro/sinatra/mini/
├── integration/   # Testes de integração com @SpringBootTest
├── service/       # Testes unitários de services
├── controller/    # Testes de erro de controllers
├── entity/        # Testes de anotações JPA
└── validation/    # Testes de validação de dados
```

### 5.2 Integration Tests
✅ Usar `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`  
✅ Limpar dados no `@BeforeEach`: deletar `ITEM_PEDIDO` primeiro (FK constraint)  
✅ Usar `TestRestTemplate` para chamadas HTTP  
✅ Para validação em PostgreSQL, usar `TestContainers` com profile dedicado (`test-postgres`)  
Exemplo: [PedidoCsvExportIntegrationTest.java](../src/test/java/pro/sinatra/mini/integration/PedidoCsvExportIntegrationTest.java)

## 6. Comandos Essenciais

### Build & Test
```bash
./mvnw clean install           # Build completo com testes
./mvnw test                    # Executar apenas testes
./mvnw spring-boot:run         # Rodar aplicação (profile: dev)
```

### Testes em PostgreSQL (TestContainers)
```bash
# Executar testes de integração contra Postgres em TestContainers
./mvnw -DskipUnitTests=false -Dspring.profiles.active=test-postgres test
```

### Flyway (migrations)
```bash
# Aplicar migrations localmente
./mvnw flyway:migrate
```

## 7. Documentação Técnica

Para decisões arquiteturais detalhadas, consultar:
- [README.md](../README.md) - Visão geral da arquitetura, endpoints e stack
- [ROADMAP-ARQUITETURA.md](../documentacao/evolucao-arquitetura/ROADMAP-ARQUITETURA.md) - Evolução v1.0.0 → v3.0.0
- [story-1.1-indices-compostos.md](../docs/story-1.1-indices-compostos.md) - Estratégias de performance e índices compostos

## 8. Critérios de Aceitação ("Done")

✅ Código compila e roda sem erros  
✅ Testes passam (`./mvnw test`)  
✅ Compatibilidade verificada contra PostgreSQL (TestContainers)  
✅ Migrations gerenciadas por Flyway  
✅ Sem violações de performance (sem full table scans, sem `findAll()` em tabelas grandes)  
✅ Código legível com responsabilidades claras por camada (SRP)  
✅ Índices compostos criados para queries filtradas