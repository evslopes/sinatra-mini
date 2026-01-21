# Sinatra Mini - SaaS de Gestão de Vendas

Sistema SaaS simplificado para gestão de vendas de múltiplas lojas, com foco em desempenho, escalabilidade e eficiência de memória.

## Stack Tecnológica

- **Linguagem**: Java 17
- **Framework**: Spring Boot 3.2.12
- **Banco de Dados**: H2 (in-memory)
- **Build**: Maven
- **Persistência**: JPA / Hibernate

## Arquitetura

O projeto segue uma arquitetura em camadas:

- **Controller**: Endpoints REST (SRP - Single Responsibility Principle)
- **Service**: Lógica de negócio, conversões de fuso horário
- **Repository**: Consultas otimizadas com projeções e streaming
- **Entity**: Modelos de dados com índices compostos

## APIs Implementadas

### 2.1 Exportação de Relatório de Vendas Diárias (CSV)

**Endpoint**: `GET /pedidos/{lojaId}/reports/daily-sales.csv`

**Descrição**: Exporta relatório de vendas diárias em formato CSV com streaming para evitar sobrecarga de memória.

**Resposta**: Arquivo CSV com cabeçalho `date,total` e linhas formatadas `YYYY-MM-DD,valor`.

**Otimizações**:
- Uso de `Stream<DailySalesProjection>` para processar dados em chunks
- Projeção JPA com apenas as colunas necessárias (dia, total)
- Streaming direto para `StreamingResponseBody` sem carregar tudo na RAM
- `@QueryHints` com `fetchSize=500` para controle de memória

### 2.2 Maior Intervalo Sem Vendas

**Endpoint**: `GET /pedidos/{lojaId}/reports/longest-no-sales?start={date}&end={date}`

**Descrição**: Calcula o maior intervalo contínuo (em dias) sem vendas registradas para uma loja.

**Parâmetros**:
- `lojaId`: ID da loja
- `start` (opcional): Data inicial do período
- `end` (opcional): Data final do período

**Resposta**:
```json
{
  "inicio": "2026-01-10",
  "fim": "2026-01-15",
  "dias": 5
}
```

**Algoritmo**: Varredura linear O(N) sobre dias distintos com vendas, calculando gaps entre datas consecutivas.

### 2.3 Dia com Menor Venda

**Endpoint**: `GET /pedidos/{lojaId}/reports/min-day-sales?start={date}&end={date}&zoneId={timezone}`

**Descrição**: Identifica o dia com o menor total de vendas no período especificado.

**Parâmetros**:
- `lojaId`: ID da loja
- `start` (opcional): Data inicial
- `end` (opcional): Data final
- `zoneId` (opcional): Fuso horário (padrão: UTC)

**Resposta**:
```json
{
  "dia": "2026-01-12",
  "total": 1250.50,
  "zoneId": "America/Sao_Paulo"
}
```

**Conversão de Timezone**: Realizada exclusivamente na camada de Service para garantir consistência.

### 2.4 Média de Vendas por Dia da Semana

**Endpoint**: `GET /pedidos/{lojaId}/reports/weekly-average?start={date}&end={date}&zoneId={timezone}`

**Descrição**: Calcula a média de vendas diárias agrupada por dia da semana (Segunda a Domingo).

**Parâmetros**:
- `lojaId`: ID da loja
- `start` (opcional): Data inicial
- `end` (opcional): Data final
- `zoneId` (opcional): Fuso horário (padrão: UTC)

**Resposta**:
```json
[
  { "diaSemana": "MONDAY", "media": 5230.75 },
  { "diaSemana": "TUESDAY", "media": 4890.20 },
  ...
]
```

## Otimizações de Performance

### Índices Compostos

Todos os endpoints de relatório utilizam filtros por `loja_id` e `data_pedido`. Foram criados índices compostos na entidade `Pedido`:

```java
@Table(name = "PEDIDO", indexes = {
    @Index(name = "idx_loja_data", columnList = "loja_id, data_pedido")
})
```

### Projeções Spring Data

Para evitar carregar entidades completas, todas as consultas de relatório utilizam **projeções** (interfaces ou DTOs) que selecionam apenas as colunas necessárias:

- `DailySalesProjection`: dia, total
- `DailySaleDaysProjection`: dia
- `SaleProjection`: dataPedido, total

### Streaming e FetchSize

Para exportações CSV e processamento de grandes volumes:

```java
@QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "500"))
Stream<DailySalesProjection> streamDailySalesByLoja(@Param("lojaId") Long lojaId);
```

### Transações Read-Only

Métodos de leitura marcados com `@Transactional(readOnly = true)` para otimizar conexões e permitir streaming.

## Decisões Técnicas

### 1. Manipulação de Datas e Fusos Horários

- **Armazenamento**: Datas no banco são tratadas de forma agnóstica (sem fuso)
- **Conversões**: Executadas somente na camada de Service
- **API**: Endpoints que dependem de timezone recebem parâmetro `zoneId` opcional

### 2. Algoritmos de Alto Desempenho

- **Complexidade Linear O(N)**: Evitado O(N²) em processamento de listas
- **Varredura única**: Cálculo de intervalos sem múltiplas iterações
- **Streaming**: Processamento incremental sem carregar tudo na memória

### 3. DTOs Imutáveis

Utilização de Java Records (Java 17) para DTOs:

```java
public record PeriodoDTO(LocalDate inicio, LocalDate fim, long dias) {}
public record MenorVendaDiaDTO(LocalDate dia, Double total, String zoneId) {}
public record MediaSemanalDTO(DayOfWeek diaSemana, Double media) {}
```

### 4. Separação de Responsabilidades

- **Controllers**: Recebem requisições e retornam respostas (sem lógica de negócio)
- **Services**: Implementam regras de negócio, cálculos e conversões
- **Repositories**: Consultas otimizadas e streaming

## Testes

A suíte de testes inclui:

- **Testes de Integração**: Validam endpoints REST end-to-end
- **Testes de Performance**: Verificam eficiência com grandes volumes (500+ pedidos)
- **Testes de Unidade**: Validam lógica de negócio em Services

### Executar Testes

```bash
mvn test
```

### Executar Teste Específico

```bash
mvn test -Dtest=PedidoCsvExportIntegrationTest
```

## Build e Execução

### Compilar

```bash
mvn clean package
```

### Executar

```bash
mvn spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`.

## Console H2

Durante a execução, o console H2 está disponível em `http://localhost:8080/h2-console`:

- **JDBC URL**: `jdbc:h2:mem:testdb`
- **User**: `sa`
- **Password**: (vazio)

## Estrutura do Projeto

```
sinatra-mini/
├── src/main/java/pro/sinatra/mini/
│   ├── controller/       # Endpoints REST
│   ├── service/          # Lógica de negócio
│   ├── repository/       # Consultas JPA
│   ├── entity/           # Modelos de dados
│   └── reports/          # DTOs de relatórios
├── src/main/resources/
│   ├── application.properties
│   └── data.sql          # Dados iniciais
├── src/test/java/        # Testes
└── docs/                 # Documentação técnica
    ├── story-1.1-indices-compostos.md
    ├── story-2.1-relatorio-csv.md
    ├── story-2.2-maior-intervalo-sem-vendas.md
    ├── story-2.3-dia-de-menor-venda.md
    └── story-2.4-media-semanal.md
```

## Documentação das Stories

Cada funcionalidade implementada está documentada em sua própria story:

### Épico 1: Otimização Estrutural (Foundation)

- **[Story 1.1 - Índices Compostos](docs/story-1.1-indices-compostos.md)**
  - Implementação de índices compostos `(loja_id, data_pedido)`
  - Otimização de queries para O(log N)
  - Validação via EXPLAIN PLAN

### Épico 2: APIs Analíticas (Core Business)

- **[Story 2.1 - Relatório CSV](docs/story-2.1-relatorio-csv.md)**
  - Exportação de vendas diárias em CSV via streaming
  - `GET /pedidos/{lojaId}/reports/daily-sales.csv`
  - Processamento eficiente de grandes volumes sem sobrecarga de memória

- **[Story 2.2 - Maior Intervalo Sem Vendas](docs/story-2.2-maior-intervalo-sem-vendas.md)**
  - Identificação do maior período sem vendas
  - `GET /pedidos/{lojaId}/reports/longest-no-sales`
  - Algoritmo O(N) sobre dias com vendas

- **[Story 2.3 - Dia de Menor Venda](docs/story-2.3-dia-de-menor-venda.md)**
  - Dia com menor total de vendas no período
  - `GET /pedidos/{lojaId}/reports/min-day-sales`
  - Suporte a fusos horários via parâmetro `zoneId`

- **[Story 2.4 - Média Semanal](docs/story-2.4-media-semanal.md)**
  - Média de vendas por dia da semana (Segunda-Domingo)
  - `GET /pedidos/{lojaId}/reports/weekly-average`
  - Agregação por `DayOfWeek` com conversão de timezone

## Melhorias Futuras (Sugestões)

1. **Cache**: Implementar cache (Redis/Caffeine) para consultas frequentes
2. **Particionamento**: Estratégia de sharding por `loja_id` para escalabilidade horizontal
3. **Processamento Assíncrono**: Filas (RabbitMQ/Kafka) para exportações grandes
4. **API Gateway**: Centralizar roteamento e autenticação em multi-tenancy
5. **Métricas**: Prometheus + Grafana para observabilidade

## Critérios de Aceitação ("Done")

Uma tarefa está completa quando:

- ✅ O código compila e roda sem erros
- ✅ Não existem violações de performance (full table scans evitados)
- ✅ Testes de integração passam
- ✅ Código é legível, com nomes significativos e responsabilidade clara por camada

## Autor

Elvis Herllain, desenvolvido como parte do desafio técnico Sinatra Mini.

## Licença

Este projeto é de uso acadêmico/avaliativo.
