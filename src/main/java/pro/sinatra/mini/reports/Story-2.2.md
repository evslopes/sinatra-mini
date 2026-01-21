# Story 2.2 — Maior Intervalo sem Vendas

Resumo
------
Como Analista, quero identificar o maior período contínuo sem vendas para uma `lojaId`, de forma eficiente (O(N) sobre os dias com vendas), para detectar falhas operacionais ou sazonalidade.

Critérios de aceite
-------------------
- Endpoint REST que recebe `lojaId`, `dataInicio` (opcional) e `dataFim` (opcional) e retorna o maior intervalo sem vendas (dataInicial, dataFinal, duração em dias).
- Implementação na camada `Service` usando algoritmo O(N) sobre uma lista ordenada de dias com vendas (projeção do repositório).
- Query otimizada: projection que retorna apenas `date(p.dataPedido)` distinto para a loja (index-friendly) — sem carregar `Pedido` completo.
- Testes unitários e de integração: cobertura de casos (nenhuma venda, vendas contínuas, múltiplos intervalos, limites de datas).

Tarefas técnicas
----------------
- Criar `DailySaleDaysProjection` no `PedidoRepository` que retorne `LocalDate` distinto por `lojaId`.
- Service: `MaiorIntervaloSemVendasService` com método `Periodo semVendas(Long lojaId, LocalDate inicio, LocalDate fim)` usando varredura linear.
- Controller: endpoint `GET /pedidos/{lojaId}/reports/longest-no-sales?start=...&end=...` retornando DTO `PeriodoDTO(record start, end, days)`.
- Tests: unit para algoritmo O(N) (listas simuladas) + integração que popula dados e valida resultado.

Notas
-----
- Reuse padrões de projeto do repositório (projections + Stream/Streamable quando apropriado).
- Garantir índices por `loja_id` + `data_pedido` (Story 1.1 já aplicou esse índice).
