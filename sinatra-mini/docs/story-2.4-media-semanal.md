# Story 2.4: Média Semanal

## Contexto
Como Gerente, quero a média de vendas por dia da semana (Seg-Dom) para identificar padrões semanais e otimizar estratégias comerciais.

## Épico
Épico 2: APIs Analíticas (Core Business)

## Objetivo
Calcular a média de vendas por dia da semana (Segunda–Domingo) para uma loja dentro de um intervalo de datas, apresentando zeros para dias sem vendas.

## Tarefas Técnicas
- [x] Repository: projection `WeeklyAggregateProjection` agregando por `DAY_OF_WEEK`
- [x] Service: cálculo de média por dia + preenchimento de dias sem vendas com 0
- [x] Controller: endpoint `GET /pedidos/{lojaId}/reports/weekly-average`
- [x] DTO: `MediaSemanalDTO` (Java record)
- [x] Testes: unitários e de integração

## Critérios de Aceitação

✅ **Resposta Completa**
- Lista de 7 entradas (MON..SUN) sempre retornada
- Dias sem vendas aparecem com média 0.0
- Ordenação Segunda a Domingo

✅ **Precisão dos Dados**
- Valores de média com precisão de 2 casas decimais
- Média = soma total / quantidade de ocorrências

✅ **Performance**
- Usa projection JPQL/SQL (não carrega entidades)
- Conversões de timezone na camada Service
- Streaming para grandes volumes

## Implementação

### Código Principal

**Repository**
```java
interface WeeklyAggregateProjection {
    Integer getDayOfWeek();
    Double getTotal();
    Long getCount();
}

@Query("SELECT DAY_OF_WEEK(p.dataPedido) as dayOfWeek, " +
       "SUM(p.total) as total, COUNT(p) as count " +
       "FROM Pedido p WHERE p.loja.id = :lojaId " +
```bash
mvn test -Dtest=MediaSemanalServiceTest
mvn test -Dtest=PedidoWeeklyAverageIntegrationTest
```

### 2. Testar Endpoint
```bash
mvn spring-boot:run

curl -sS "http://localhost:8080/pedidos/1/reports/weekly-average?start=2025-01-01&end=2025-01-31&zoneId=UTC" | jq
```

**Resposta esperada:**
```json
[
  {"dayOfWeek": "MONDAY", "average": 1520.30},
  {"dayOfWeek": "TUESDAY", "average": 1430.50},
  {"dayOfWeek": "WEDNESDAY", "average": 0.0},
  {"dayOfWeek": "THURSDAY", "average": 1680.75},
  {"dayOfWeek": "FRIDAY", "average": 1890.20},
  {"dayOfWeek": "SATURDAY", "average": 2100.00},
  {"dayOfWeek": "SUNDAY", "average": 0.0}
]
```

## Status & Links

- **Issue**: Parte do backlog
- **PR**: A ser criado
- **Branch**: `feature/story-2.4`

## Labels Aplicadas
- `analytics`
- `performance`
- `aggregation`

## Observações

### Implementação e Performance
- Função `DAY_OF_WEEK()` para H2 (compatibilidade)
- Stream com `@QueryHints` para grandes volumes
- Conversões de timezone no Service usando `ZonedDateTime`

### Arquivos Relevantes
- Repository: `src/main/java/pro/sinatra/mini/repository/PedidoRepository.java`
- Service: `src/main/java/pro/sinatra/mini/service/MediaSemanalService.java`
- DTO: `src/main/java/pro/sinatra/mini/reports/MediaSemanalDTO.java`
- Controller: `src/main/java/pro/sinatra/mini/controller/PedidoController.java`
- Testes: `src/test/java/pro/sinatra/mini/service/MediaSemanalServiceTest.java`
- Testes: `src/test/java/pro/sinatra/mini/integration/PedidoWeeklyAverageIntegrationTest.java`

### Dependências
- Story 1.1 (índices) para performance em filtros por loja e data

## Checklist de Conclusão

- [x] Projection com agregação por dia da semana
- [x] Service calculando médias corretamente
- [x] Endpoint REST com parâmetros opcionais
- [x] Testes unitários e de integração
- [x] Documentação atualizada
- [ ] Code review e merge (pendente)

---

**Critério de "Done":**
✅ Lista de 7 dias sempre retornada
✅ Médias calculadas corretamente
✅ Dias sem vendas com valor 0.0
mvn -Dtest=pro.sinatra.mini.integration.PedidoWeeklyAverageIntegrationTest test -Dspring.profiles.active=test
```

Dividir entre `Repository`/`Service`/`Controller`.

Dependências:

- Story 1.1 (índices) deve estar presente para garantir performance em filtros por loja e data.

Deliverables:

- `WeeklyAggregateProjection` + repository streaming method
- `MediaSemanalService`, `MediaSemanalDTO`
- Endpoint `GET /pedidos/{lojaId}/reports/weekly-average`
- Unit + integration tests
- Documentação atualizada em `docs/story-2.4-media-semanal.md` e PR criado
