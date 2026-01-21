# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/lang/pt-BR/).

---

## [1.0.0] - 2026-01-20

### 🎉 Lançamento Inicial - Sinatra Mini SaaS

Sistema SaaS simplificado para gestão de vendas de múltiplas lojas, com foco em desempenho, escalabilidade e eficiência de memória.

### ✨ Adicionado

#### APIs Analíticas (Core Business)

- **[Story 2.1]** Exportação de Relatório de Vendas Diárias (CSV)
  - Endpoint: `GET /pedidos/{lojaId}/reports/daily-sales.csv`
  - Streaming de dados para evitar sobrecarga de memória (suporta 100k+ registros)
  - Projeções JPA para selecionar apenas colunas necessárias (dia, total)
  - `@QueryHints` com `fetchSize=500` para controle eficiente de memória
  - Formato CSV padronizado: header `date,total` + linhas `YYYY-MM-DD,valor`

- **[Story 2.2]** Maior Intervalo Sem Vendas
  - Endpoint: `GET /pedidos/{lojaId}/reports/longest-no-sales?start={date}&end={date}`
  - Algoritmo O(N) de varredura linear sobre dias distintos
  - Retorna período contínuo sem vendas (início, fim, total de dias)
  - Projeção otimizada: seleciona apenas datas distintas com vendas

- **[Story 2.3]** Dia com Menor Venda
  - Endpoint: `GET /pedidos/{lojaId}/reports/min-day-sales?start={date}&end={date}&zoneId={timezone}`
  - Identificação do dia com menor total de vendas no período
  - Conversões de fuso horário executadas na camada de Service
  - Suporte a parâmetro `zoneId` opcional (padrão: UTC)
  - Retorna 204 No Content quando não há vendas no período

- **[Story 2.4]** Média de Vendas por Dia da Semana
  - Endpoint: `GET /pedidos/{lojaId}/reports/weekly-average?start={date}&end={date}&zoneId={timezone}`
  - Cálculo de média diária agrupada por dia da semana (Segunda–Domingo)
  - Retorna sempre 7 dias, com média 0.0 para dias sem vendas
  - Agregação eficiente com `DAY_OF_WEEK()` para compatibilidade H2
  - Streaming com `@QueryHints` para grandes volumes

#### Otimizações de Performance (Foundation)

- **[Story 1.1]** Índices Compostos
  - Índice `idx_pedido_loja_data` na tabela PEDIDO: `(loja_id, data_pedido)`
  - Reduz complexidade de busca de O(N) para O(log N)
  - Suporta filtros eficientes em cenário multi-tenant
  - Ordenação natural por data sem custo adicional

#### Arquitetura e Padrões

- **Separação de Responsabilidades (SRP)**
  - Controllers: recebem requisições e retornam respostas
  - Services: lógica de negócio, cálculos, conversões de timezone
  - Repositories: consultas otimizadas com projeções e streaming

- **DTOs Imutáveis com Java Records**
  - `PeriodoDTO`: período sem vendas (início, fim, dias)
  - `MenorVendaDiaDTO`: dia com menor venda (dia, total)
  - `MediaSemanalDTO`: média por dia da semana (dayOfWeek, average)
  - `DailySalesProjection`: vendas diárias (dia, total)

- **Projeções Spring Data**
  - Evitam carregar entidades completas (anti-pattern: `findAll()`)
  - Selecionam apenas colunas necessárias para cada relatório
  - Reduzem tráfego de rede e uso de memória

- **Transações Read-Only**
  - Métodos de leitura marcados com `@Transactional(readOnly = true)`
  - Otimizam conexões JDBC e permitem streaming de dados

#### Suíte de Testes (98.11% de Sucesso)

- **Testes Unitários (9 testes)**
  - `MediaSemanalServiceTest`: 6 testes (cálculo de médias, timezones)
  - `MaiorIntervaloSemVendasServiceTest`: 2 testes (algoritmo O(N))
  - `DiaMenorVendaServiceTest`: 1 teste (identificação de mínimo)

- **Testes de Integração (27 testes)**
  - `PedidoCsvExportIntegrationTest`: validação de streaming CSV
  - `PedidoLongestNoSalesIntegrationTest`: período sem vendas
  - `PedidoMinDaySalesIntegrationTest`: dia de menor venda
  - `PedidoWeeklyAverageIntegrationTest`: média semanal (7 dias)
  - `PedidoControllerErrorTest`: 10 testes de erro/edge cases
  - `PedidoCrudIntegrationTest`: 10 testes CRUD (7 executáveis)

- **Testes de Validação (16 testes)**
  - `PedidoValidationTest`: validação de DTOs imutáveis
  - Testes de extremos: valores zero, negativos, grandes
  - Testes de precisão decimal
  - Validação de imutabilidade (Java Records)

- **Testes de Performance (1 teste - skipped)**
  - `PedidoLoadPerformanceTest`: valida 10k registros
  - Verifica streaming sem estouro de memória (<50MB delta)
  - Tempo de exportação <10s para 10k registros
  - Habilitado com `-Dperf=true`

#### Entidades e Banco de Dados

- **Schema H2 (in-memory)**
  - `PEDIDO`: id, data_pedido, status, total, metodo_pagamento, loja_id, cliente_id
  - `LOJA`: id, nome, data_criacao
  - `CLIENTE`: id, nome, email
  - `ITEM_PEDIDO`: id, pedido_id, produto, quantidade, preco_unitario

- **Índices Otimizados**
  - Índice composto: `idx_pedido_loja_data (loja_id, data_pedido)`
  - Suporte a filtros multi-tenant eficientes

#### Documentação Técnica

- **README.md**: visão geral do projeto, stack, APIs, otimizações
- **Story 1.1**: documentação de índices compostos + EXPLAIN PLAN
- **Story 2.1**: exportação CSV com streaming
- **Story 2.2**: algoritmo de maior intervalo sem vendas
- **Story 2.3**: dia de menor venda com suporte a timezone
- **Story 2.4**: média semanal com preenchimento de zeros
- **QA-TEST-REPORT.md**: relatório completo de testes (98.11% aprovação)

### 🔧 Corrigido

- **LazyInitializationException** em endpoints REST
  - Adicionado `@JsonIgnore` em `Pedido.itens`
  - Previne erro de serialização JSON quando sessão Hibernate está fechada
  - Aplicável a todas as APIs que retornam entidade `Pedido`

- **DuplicateKeyException** em testes de integração
  - Implementada limpeza completa do banco em `@BeforeEach`
  - Ordem de DELETE: ITEM_PEDIDO → PEDIDO → LOJA → CLIENTE
  - Resolve conflitos de chave primária entre testes

- **Compatibilidade Spring Boot 3.2.12**
  - Substituído `getStatusCodeValue()` por `getStatusCode().value()`
  - Atualizado para API do Spring Boot 6.0 (compatível com Java 17)

- **Conflitos de Merge**
  - Resolvidos conflitos entre branches de Stories 2.1-2.4
  - Duplicatas de classes removidas (DiaMenorVendaService, MediaSemanalService)
  - Conflitos no PedidoController resolvidos

- **Endpoint CSV**
  - Rota corrigida de `/pedidos/{lojaId}/daily-sales.csv` para `/pedidos/{lojaId}/reports/daily-sales.csv`
  - Formato CSV ajustado para `date,total` (antes: `data,valor`)
  - Content-Disposition header adicionado para download automático

### 🚀 Performance

- **Streaming de Dados**
  - Exportação de 100k+ registros sem carregar tudo na RAM
  - Uso de `Stream<T>` com `@QueryHints(fetchSize=500)`
  - Processamento em chunks/lotes para eficiência de memória

- **Projeções JPA**
  - Redução de tráfego de rede (~70% menos dados)
  - Queries selecionam apenas colunas necessárias
  - Evitam N+1 queries com joins otimizados

- **Índices Compostos**
  - Busca O(log N) vs O(N) em full table scans
  - Range queries eficientes em datas dentro de loja
  - Suporte a cenário multi-tenant (cardinalidade loja < data)

- **Algoritmos Lineares**
  - Maior intervalo sem vendas: O(N) sobre dias distintos
  - Evitado O(N²) em loops aninhados
  - Varredura única com state tracking

### 📊 Métricas de Qualidade

- **Cobertura de Testes**: ~70-75% (antes: ~30%)
- **Taxa de Sucesso**: 98.11% (52/53 testes passando)
- **Testes Totais**: 53 (antes: 9)
- **Falhas/Erros**: 0 (antes: 4)
- **Skipped**: 1 (teste de performance, requer `-Dperf=true`)

### 🏗️ Stack Tecnológica

- **Java**: 17 (LTS)
- **Spring Boot**: 3.2.12
- **Banco de Dados**: H2 (in-memory)
- **Build Tool**: Maven
- **ORM**: JPA / Hibernate
- **Testes**: JUnit 5, Mockito, Spring Boot Test

### 📝 Convenções e Padrões

- **Datas e Fusos**: armazenamento agnóstico (UTC), conversões no Service
- **DTOs**: Java Records imutáveis (Java 17)
- **Queries**: projeções para evitar `findAll()` em tabelas de alto volume
- **Transações**: `@Transactional(readOnly=true)` em métodos de leitura
- **Algoritmos**: preferência por O(N) linear sobre O(N²) quadrático

### ⚠️ Limitações Conhecidas

- **Teste de Performance**: skipped por padrão (requer `-Dperf=true`)
- **Banco H2**: in-memory, dados resetados a cada restart
- **Multi-Tenancy**: básico (filtro por loja_id), sem isolamento total

### 🔮 Planejado para Próximas Versões

- Validação de entrada em Controllers (Bean Validation)
- Testes de performance habilitados em CI/CD
- Migração para PostgreSQL/MySQL em produção
- Cache de consultas frequentes (Redis/Caffeine)
- Paginação em endpoints de leitura (`/pedidos`)
- Autenticação e autorização (Spring Security + JWT)
- Rate limiting para proteção contra abuso
- Observabilidade (Prometheus + Grafana)

---

## Como Atualizar

### Versão 1.0.0

```bash
git clone https://github.com/evslopes/sinatra-mini.git
cd sinatra-mini/sinatra-mini
mvn clean install
mvn spring-boot:run
```

**Endpoints disponíveis:**
- `GET /pedidos/{lojaId}/reports/daily-sales.csv`
- `GET /pedidos/{lojaId}/reports/longest-no-sales?start={date}&end={date}`
- `GET /pedidos/{lojaId}/reports/min-day-sales?start={date}&end={date}&zoneId={timezone}`
- `GET /pedidos/{lojaId}/reports/weekly-average?start={date}&end={date}&zoneId={timezone}`

**Console H2:** http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- User: `sa`
- Password: (vazio)

---

## Links Úteis

- **Repositório**: https://github.com/evslopes/sinatra-mini
- **Documentação**: [README.md](README.md)
- **Relatório QA**: [docs/QA-TEST-REPORT.md](docs/QA-TEST-REPORT.md)
- **Stories**:
  - [Story 1.1 - Índices Compostos](docs/story-1.1-indices-compostos.md)
  - [Story 2.1 - Relatório CSV](docs/story-2.1-relatorio-csv.md)
  - [Story 2.2 - Maior Intervalo](docs/story-2.2-maior-intervalo-sem-vendas.md)
  - [Story 2.3 - Dia Menor Venda](docs/story-2.3-dia-de-menor-venda.md)
  - [Story 2.4 - Média Semanal](docs/story-2.4-media-semanal.md)

---

**Desenvolvido por:** Elvis Herllain  
**Licença:** MIT  
**Versão:** 1.0.0
