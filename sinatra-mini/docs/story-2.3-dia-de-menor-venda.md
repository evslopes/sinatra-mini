# Story 2.3: Dia de Menor Venda

## Contexto
Como Gerente, quero saber o dia de pior desempenho em vendas para identificar padrões e planejar ações corretivas.

## Épico
Épico 2: APIs Analíticas (Core Business)

## Objetivo
Identificar, por loja, o dia com menor valor de vendas dentro de um intervalo de datas, respeitando o fuso horário do usuário.

## Tarefas Técnicas
- [x] Repository: projection `SaleProjection` + método `streamSalesByLoja`
- [x] Service: agregação por `LocalDate` com conversão de timezone
- [x] Controller: endpoint `GET /pedidos/{lojaId}/reports/min-day-sales`
- [x] DTO: `MenorVendaDiaDTO` (Java record)
- [x] Testes: unitários e de integração

## Critérios de Aceitação

✅ **Resposta Correta**
- Retorna `200 OK` com `MenorVendaDiaDTO` quando existem vendas
- Retorna `204 No Content` quando não há vendas no intervalo

✅ **Timezone Handling**
- Conversões de fuso acontecem na camada de Service
- Banco permanece agnóstico quanto ao timezone
- Suporte a parâmetro `zoneId`

✅ **Performance**
- Usa projections + streaming para evitar OOM
- Agregação eficiente por dia (algoritmo linear O(N))

## Status & Links

- **Issue**: #10
- **PR**: #5 (merged)
- **Branch**: `main`

## Labels Aplicadas
- `analytics`
- `performance`
- `timezone`

## Implementação

### Código Principal

**Repository**
```java
interface SaleProjection {
    LocalDateTime getDataPedido();
    Double getTotal();
}

@Query("SELECT p.dataPedido as dataPedido, p.total as total " +
       "FROM Pedido p WHERE p.loja.id = :lojaId " +
       "AND (:start IS NULL OR p.dataPedido >= :start) " +
       "AND (:end IS NULL OR p.dataPedido <= :end)")
Stream<SaleProjection> streamSalesByLoja(...);
```

**Service**
```java
public MenorVendaDiaDTO calcularDiaMenorVenda(Long lojaId, LocalDate inicio, LocalDate fim, ZoneId zoneId) {
    // Converte timestamps para ZoneId do usuário
    // Agrega por LocalDate no timezone correto
    // Retorna dia com menor total (algoritmo O(N))
}
```

## Como Validar Localmente

### 1. Executar Testes
```bash
mvn test -Dtest=DiaMenorVendaServiceTest
mvn test -Dtest=PedidoMinDaySalesIntegrationTest
```

### 2. Testar Endpoint
```bash
mvn spring-boot:run

curl -sS "http://localhost:8080/pedidos/1/reports/min-day-sales?start=2025-01-01&end=2025-01-31&zoneId=America/Sao_Paulo" | jq
```

**Resposta esperada:**
```json
{
  "dia": "2025-01-15",
  "total": 1250.50
}
```

## Observações

### Implementação e Performance
- Agregação por `LocalDate` usando `ZonedDateTime` para conversões seguras
- Stream com `@QueryHints(HINT_FETCH_SIZE)` para grandes volumes
- Algoritmo linear O(N) sobre registros do período

### Arquivos Relevantes
- Repository: `src/main/java/pro/sinatra/mini/repository/PedidoRepository.java`
- Service: `src/main/java/pro/sinatra/mini/service/DiaMenorVendaService.java`
- DTO: `src/main/java/pro/sinatra/mini/reports/MenorVendaDiaDTO.java`
- Controller: `src/main/java/pro/sinatra/mini/controller/PedidoController.java`
- Testes: `src/test/java/pro/sinatra/mini/service/DiaMenorVendaServiceTest.java`
- Testes: `src/test/java/pro/sinatra/mini/integration/PedidoMinDaySalesIntegrationTest.java`

## Checklist de Conclusão

- [x] Projection e query otimizada
- [x] Service com conversão de timezone
- [x] Endpoint REST com parâmetro zoneId
- [x] Testes unitários e de integração
- [x] Documentação atualizada
- [x] Code review e merge

---

**Critério de "Done":**
✅ Endpoint retornando dia correto com timezone
✅ Testes de integração passando
✅ Conversões de fuso na camada Service