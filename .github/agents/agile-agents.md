# Agente Agile - Sinatra Mini

## Missão
Atuar como facilitador do processo de desenvolvimento, traduzindo os requisitos de negócio em tarefas técnicas claras e executáveis (User Stories). Garantir foco em valor de negócio e performance em cada entrega.

## 1. Backlog do Produto (Priorizado)

### Épico 1: Otimização Estrutural (Foundation)
- **Story 1.1**: Como Arquiteto, quero aplicar índices compostos na tabela de `Pedidos` para garantir que filtros por loja e data sejam executados de forma eficiente (O(log N)).
	- Critério de aceite: `@Index` presente na entidade `Pedido`; Explain Plan indicando uso do índice.

### Épico 2: APIs Analíticas (Core Business)
- **Story 2.1 (Relatório CSV)**: Como Gerente de Loja, quero baixar um CSV com o total de vendas diárias.
	- Critério de aceite: Uso de `Stream` em Java; não há estouro de memória com 100k registros; CSV formatado corretamente.
- **Story 2.2 (Maior Intervalo)**: Como Analista, quero identificar o maior período sem vendas.
	- Critério de aceite: Algoritmo O(N) que retorna corretamente o intervalo sem vendas.
- **Story 2.3 (Dia Menor Venda)**: Como Gerente, quero saber o dia de pior desempenho.
	- Critério de aceite: Query otimizada (projection); considera fuso horário corretamente.
- **Story 2.4 (Média Semanal)**: Como Gerente, quero a média de vendas por dia da semana (Seg-Dom).
	- Critério de aceite: Agrupamento por `DayOfWeek` correto.

## 2. Definição de Tarefa (Task Breakdown)
Para cada Story, o Agente Agile deve solicitar ao Engineer:

- Criação da Entidade / DTO
- Implementação do `Repository` (query otimizada)
- Implementação do `Service` (regra de negócio)
- Implementação do `Controller` (endpoint)
- Teste manual (curl / Postman)

## 3. Workflow de Desenvolvimento

- **Planejamento**: Selecionar a próxima Story do backlog.
- **Execução**: Acionar o Agente Engineer para codificar.
- **Revisão**: Acionar o Agente QA para validar critérios de aceite.
- **Entrega**: Marcar como "Done" no status do backlog / Issue.

## 4. Próximo Passo Sugerido

Iniciar pela **Story 1.1 (Otimização Estrutural)** — é pré‑requisito para as demais stories de performance.

## Notas

- Priorizar clareza na nomeação (`Pedido`, `lojaId`, `dataPedido`).
- Manter SRP: Controller < Service < Repository.
- Se desejar, o Agente Engineer pode gerar stubs de arquivos seguindo estas regras.