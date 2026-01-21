# Story 2.2: Maior Intervalo Sem Vendas

## Contexto
Como Analista, quero identificar o maior período sem vendas para entender padrões de sazonalidade e planejar ações comerciais.

## Épico
Épico 2: APIs Analíticas (Core Business)

## Objetivo
Identificar o maior período contínuo sem vendas por `lojaId` de forma eficiente (algoritmo O(N) sobre dias com vendas).

## Tarefas Técnicas
- [x] Repository: projection `DailySaleDaysProjection` retornando apenas dias com vendas
- [x] Service: algoritmo O(N) de varredura linear calculando gaps entre datas
- [x] Controller: endpoint `GET /pedidos/{lojaId}/reports/longest-no-sales`
- [x] DTO: `PeriodoDTO` (Java record)
- [x] Testes: unitários e de integração

## Critérios de Aceitação

✅ **Algoritmo Eficiente**
- Complexidade O(N) sobre número de dias com vendas
- Não processa todos os pedidos, apenas dias distintos

✅ **Resposta Correta**
- Retorna `PeriodoDTO` com `inicio`, `fim` e `dias`
- Considera parâmetros opcionais `start` e `end`

✅ **Performance**
- Query otimizada com projection (seleciona apenas coluna de data)
- Usa índice composto `idx_loja_data`

## Implementação

### Código Principal

**Repository**
```java
interface DailySaleDaysProjection {
    LocalDate getDia();
}

@Query("SELECT DISTINCT cast(p.dataPedido as date) as dia " +
       "FROM Pedido p WHERE p.loja.id = :lojaId " +
       "AND (:start IS NULL OR cast(p.dataPedido as date) >= :start) " +
       "AND (:end IS NULL OR cast(p.dataPedido as date) <= :end) " +
       "ORDER BY dia")
Stream<DailySaleDaysProjection> streamDistinctSaleDaysByLoja(
    @Param("lojaId") Long lojaId,
    @Param("start") LocalDate start,
    @Param("end") LocalDate end);
```

**Service**
```java
public PeriodoDTO calcularMaiorIntervalo(Long lojaId, LocalDate inicio, LocalDate fim) {
    List<LocalDate> diasComVendas = // stream from repository
    // Algoritmo O(N): varredura linear calculando gaps
    // Retorna maior intervalo sem vendas
}
```

## Como Validar Localmente

### 1. Executar Testes
```bash
mvn test -Dtest=MaiorIntervaloSemVendasServiceTest
mvn test -Dtest=PedidoLongestNoSalesIntegrationTest
```

### 2. Testar Endpoint
```bash
mvn spring-boot:run

curl -sS "http://localhost:8080/pedidos/1/reports/longest-no-sales?start=2025-01-01&end=2025-01-31" | jq
```

**Resposta esperada:**
```json
{
  "inicio": "2025-01-10",
  "fim": "2025-01-15",
  "dias": 5
}
```

## Status & Links

- **Issue**: #9
- **PR**: #12 (merged)
- **Branch**: `main`

## Labels Aplicadas
- `analytics`
- `performance`
- `algorithm`

## Observações

### Decisões Técnicas
- Usa `cast(p.dataPedido as date)` para compatibilidade H2
- Stream de dias com vendas (não processa pedidos individuais)
- Algoritmo O(N) sobre dias (não sobre pedidos)

### Arquivos Relevantes
- Repository: `src/main/java/pro/sinatra/mini/repository/PedidoRepository.java`
- Service: `src/main/java/pro/sinatra/mini/service/MaiorIntervaloSemVendasService.java`
- DTO: `src/main/java/pro/sinatra/mini/reports/PeriodoDTO.java`
- Controller: `src/main/java/pro/sinatra/mini/controller/PedidoController.java`
- Testes: `src/test/java/pro/sinatra/mini/service/MaiorIntervaloSemVendasServiceTest.java`
- Testes: `src/test/java/pro/sinatra/mini/integration/PedidoLongestNoSalesIntegrationTest.java`

## Checklist de Conclusão

- [x] Projection e query otimizada
- [x] Service com algoritmo O(N)
- [x] Endpoint REST funcionando
- [x] Testes unitários e de integração
- [x] Documentação atualizada
- [x] Code review e merge

---

**Critério de "Done":**
✅ Endpoint retornando períodos corretos
✅ Algoritmo eficiente validado
✅ Testes passando

**Como testar localmente**
- Rodar todos os testes (unit + integração):

```bash
cd sinatra-mini
mvn test
```
- Teste rápido do endpoint (após subir a aplicação):

```bash
# executar a aplicação
mvn spring-boot:run

# chamar o endpoint (exemplo)
curl -sS "http://localhost:8080/pedidos/1/reports/longest-no-sales?start=2025-01-01&end=2025-01-31" | jq
```

**Critérios de aceite atendidos**
- Endpoint REST disponível e retornando `PeriodoDTO` com `dataInicial`, `dataFinal` e `dias`.
- Implementação do Service com algoritmo O(N) sobre dias com vendas.
- Query otimizada (projection) que seleciona apenas a coluna de data (date(p.dataPedido)).
- Testes unitários e de integração cobrindo cenários chave.

**Observações / Próximos passos**
- Garantir execução nas pipelines CI (os testes passam localmente; recomendo adicionar job para execução de integração).
- Story 2.3 e 2.4 seguem em backlog; reusar padrão de projections + Service (conversões de fuso devem ocorrer na camada Service conforme diretrizes do projeto).

**Referências**
- Pull Request: https://github.com/evslopes/sinatra-mini/pull/12
- Issue: https://github.com/evslopes/sinatra-mini/issues/9
