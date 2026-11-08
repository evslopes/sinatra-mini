# Agente Agile - Sinatra Mini

## Missão
Atuar como facilitador do processo de desenvolvimento, traduzindo os requisitos de negócio em tarefas técnicas claras e executáveis (User Stories). Garantir foco em valor de negócio e performance em cada entrega.

## 1. Backlog do Produto

### ✅ Épico 1: Otimização Estrutural (Foundation) — CONCLUÍDO
- **Story 1.1**: Como Arquiteto, quero aplicar índices compostos na tabela de `Pedidos` para garantir que filtros por loja e data sejam executados de forma eficiente (O(log N)).
	- ✅ Critério de aceite: `@Index` presente na entidade `Pedido`; Explain Plan indicando uso do índice.

### ✅ Épico 2: APIs Analíticas (Core Business) — CONCLUÍDO
- **Story 2.1 (Relatório CSV)**: Como Gerente de Loja, quero baixar um CSV com o total de vendas diárias.
	- ✅ Critério de aceite: Uso de `Stream` em Java; não há estouro de memória com 100k registros; CSV formatado corretamente.
- **Story 2.2 (Maior Intervalo)**: Como Analista, quero identificar o maior período sem vendas.
	- ✅ Critério de aceite: Algoritmo O(N) que retorna corretamente o intervalo sem vendas.
- **Story 2.3 (Dia Menor Venda)**: Como Gerente, quero saber o dia de pior desempenho.
	- ✅ Critério de aceite: Query otimizada (projection); considera fuso horário corretamente.
- **Story 2.4 (Média Semanal)**: Como Gerente, quero a média de vendas por dia da semana (Seg-Dom).
	- ✅ Critério de aceite: Agrupamento por `DayOfWeek` correto.

### 🚀 Novo Backlog (Roadmap v1.1.0 - v3.0.0)

#### Épico 1: Migração PostgreSQL + Cache Distribuído (v1.1.0)
- **Issue #15**: Migrar banco de dados de H2 para PostgreSQL em produção.
	- Subtarefas: Migration scripts, configuração de profiles, validação de compatibilidade.

#### Épico 2: Cache Distribuído Multi-Level (v1.1.0)
- **Issue #23**: Implementar cache L1 (Caffeine) local.
- **Issue #24**: Implementar cache L2 (Redis) distribuído.
	- Subtarefas: Configuração de TTL, invalidação, estratégia de warming.

#### Épico 3: Alta Disponibilidade (v1.2.0)
- **Issue #26**: Implementar estratégias de HA: load balancing, circuit breaker, health checks.
	- Subtarefas: Configuração de réplicas, failover automático, monitoramento.

#### Épico 4: Observabilidade e Monitoramento (v1.2.0)
- **Issue #30**: Integração com stack de observabilidade (Prometheus, Grafana, Jaeger).
	- Subtarefas: Métricas customizadas, dashboards, alertas, tracing distribuído.

#### Épico 5: Multi-Tenancy com Sharding (v2.0.0)
- **Issue #34**: Implementar multi-tenancy com isolamento de dados por tenant.
	- Subtarefas: Sharding strategy, tenant resolution, data isolation.

#### Épico 6: CQRS e Event Sourcing (v3.0.0)
- **Issue #39**: Separar commands e queries; implementar event sourcing para auditoria.
	- Subtarefas: Event store, projeções, eventual consistency, saga pattern.

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

### Workflow de Execução entre Agentes

```
┌──────────┐
│  Agile   │  1. Cria Story com critérios de aceite
└────┬─────┘
     │
     ▼
┌──────────┐
│ Engineer │  2. Implementa código seguindo padrões
└────┬─────┘
     │
     ▼
┌──────────┐
│  DevOps  │  3. Configura infra e deploy
└────┬─────┘
     │
     ▼
┌──────────┐
│    QA    │  4. Valida funcional + performance
└────┬─────┘
     │
     ▼
   DONE ✅
```

### Responsabilidades por Agente

| Agente | Papel | Arquivo | Responsabilidades |
|--------|-------|---------|-------------------|
| **Agile** | Product Owner | [agile-agents.md](./agile-agents.md) | Backlog, priorização, stories, critérios de aceite |
| **Engineer** | Developer | [engineer-agents.md](./engineer-agents.md) | Código, arquitetura, padrões, performance |
| **DevOps** | SRE/Infra | [devops-agents.md](./devops-agents.md) | CI/CD, infraestrutura, deploy, monitoramento |
| **QA** | Quality Assurance | [qa-agents.md](./qa-agents.md) | Testes, validação, performance, carga |

## 4. Definition of Done (DoD)

Para considerar uma Story como **DONE**, os seguintes critérios devem ser atendidos:

### Código
- [ ] **Compilação**: `./mvnw clean install` executado com sucesso
- [ ] **Testes**: Todos os testes (unitários + integração) passando
- [ ] **Cobertura**: Mínimo 80% de code coverage para novos códigos
- [ ] **Padrões**: Código segue [copilot-instructions.md](../copilot-instructions.md)
- [ ] **Review**: Aprovação de pelo menos 1 reviewer (Engineer ou Senior)

### Performance
- [ ] **Índices**: Queries otimizadas com índices compostos
- [ ] **Memória**: Sem memory leaks (validado com profiler)
- [ ] **Latência**: p95 < 100ms para endpoints críticos
- [ ] **Streaming**: Uso de `Stream<T>` para dados > 1000 registros

### DevOps
- [ ] **Branch**: Merged para `develop` via Pull Request
- [ ] **CI/CD**: Pipeline verde no GitHub Actions
- [ ] **Deploy**: Deploy em ambiente de staging bem-sucedido
- [ ] **Rollback**: Plano de rollback documentado

### Documentação
- [ ] **README**: Atualizado se houver novos comandos/configs
- [ ] **API**: Swagger/OpenAPI atualizado (se novos endpoints)
- [ ] **Architecture**: Diagramas atualizados (se mudanças estruturais)
- [ ] **CHANGELOG**: Versão e mudanças documentadas

### QA
- [ ] **Funcional**: Todos os critérios de aceite validados
- [ ] **Regressão**: Suite de regressão executada e passando
- [ ] **Performance**: Testes de carga executados (se aplicável)
- [ ] **Security**: Scan de vulnerabilidades executado (OWASP, Snyk)

## 5. Próximo Passo Sugerido

Iniciar pelos **Épicos de v1.1.0** (Migração PostgreSQL + Cache Distribuído) — são pré‑requisitos para ambientes de produção e escalabilidade horizontal.

## Notas

- Priorizar clareza na nomeação (`Pedido`, `lojaId`, `dataPedido`).
- Manter SRP: Controller < Service < Repository.
- Se desejar, o Agente Engineer pode gerar stubs de arquivos seguindo estas regras.

## Fase 1 — Progresso

- Reformatação e padronização dos arquivos de agentes:
	- `/.github/agents/engineer-agents.md` — convertido para Markdown organizado e exemplos práticos.
	- `/.github/agents/agile-agents.md` — backlog e fluxo atualizados e formatados.
- Registro das tarefas iniciais no TODO interno (planejamento e rastreio concluído).

## 6. Cronograma Proposto

| Versão | Épicos | Timeline | Status |
|--------|--------|----------|--------|
| **v1.1.0** | PostgreSQL + Redis | Q2 2026 (Abr-Jun) | ⏳ Planejado |
| **v1.2.0** | Alta Disponibilidade + Observabilidade | Q3 2026 (Jul-Set) | 📋 Backlog |
| **v2.0.0** | Sharding Multi-Tenant | Q4 2026 (Out-Dez) | 💡 Proposto |
| **v3.0.0** | CQRS + Event Sourcing | Q1 2027 (Jan-Mar) | 💡 Proposto |

## 7. Critérios de Sucesso por Versão

### v1.1.0 (PostgreSQL + Redis)
- ✅ Zero data loss após migration
- ✅ Performance igual ou melhor que H2
- ✅ Cache hit rate > 70%
- ✅ Latência p95 < 100ms

### v1.2.0 (Alta Disponibilidade)
- ✅ Uptime > 99.9% (SLA)
- ✅ Zero downtime deployments
- ✅ MTTR < 5 minutos (failover automático)
- ✅ Dashboards com alertas configurados

### v2.0.0 (Sharding)
- ✅ Suportar 10.000+ lojas
- ✅ Latência p95 < 200ms mesmo com 10k lojas
- ✅ Queries cross-shard funcionais (relatórios globais)

### v3.0.0 (CQRS)
- ✅ Writes não impactam reads
- ✅ Relatórios complexos < 500ms
- ✅ Event log completo (auditoria)

## 8. Próximas Entregas (Issues no GitHub)

### v1.1.0 (Prioritário)
- **#15**: Migração PostgreSQL + configuração de ambientes
- **#23**: Cache L1 (Caffeine) local
- **#24**: Cache L2 (Redis) distribuído

### v1.2.0 (Médio prazo)
- **#26**: Alta Disponibilidade (load balancing, circuit breaker)
- **#30**: Observabilidade (Prometheus, Grafana, Jaeger)

### v2.0.0 (Longo prazo)
- **#34**: Multi-Tenancy com Sharding

### v3.0.0 (Visão futura)
- **#39**: CQRS e Event Sourcing

**Observação**: Para detalhes completos de cada épico, consulte as issues no GitHub usando filtros `label:epic` ou `milestone:v1.1.0`.

### Comandos úteis (GitHub CLI)

```bash
# Ver todas as issues abertas
gh issue list --state open

# Filtrar por épico
gh issue list --label "epic:postgresql"

# Atribuir issue ao agente
gh issue edit 16 --add-assignee @me

# Ver detalhes de uma issue
gh issue view 16
```

## 9. Próximos Passos Imediatos

### Sprint 1 (Semana 1-2 de Fev/2026)

1. **Agile Agent**: ✅ Issues criadas no GitHub
   - [#15](https://github.com/evslopes/sinatra-mini/issues/15): Épico 1 - PostgreSQL
   - [#16](https://github.com/evslopes/sinatra-mini/issues/16): Story 1.1.1 - Setup PostgreSQL
   - [#17](https://github.com/evslopes/sinatra-mini/issues/17): Story 1.1.2 - Flyway
   - [#18](https://github.com/evslopes/sinatra-mini/issues/18): Story 1.1.3 - Testes
   - [#19](https://github.com/evslopes/sinatra-mini/issues/19): Story 1.1.4 - Backup

2. **DevOps Agent**: Iniciar Story 1.1.1 (#16)
   ```bash
   gh issue view 16  # Ver detalhes da issue
   git checkout develop
   git pull origin develop
   git checkout -b feature/1-postgresql-setup
   ```

3. **Engineer Agent**: Revisar dependências e preparar Flyway migrations

4. **QA Agent**: Preparar ambiente de testes com TestContainers

### Sprint 2 (Semana 3-4 de Fev/2026)

1. **Engineer Agent**: Implementar Stories 1.1.2 (Flyway)
2. **QA Agent**: Executar Story 1.1.3 (Testes)
3. **DevOps Agent**: Implementar Story 1.1.4 (Backup)
4. **Agile Agent**: Sprint Review + Planning do Épico 2 (Cache)

## 10. Referências

- [ROADMAP-ARQUITETURA.md](../../documentacao/evolucao-arquitetura/ROADMAP-ARQUITETURA.md) - Roadmap completo com detalhes técnicos
- [copilot-instructions.md](../copilot-instructions.md) - Instruções gerais do projeto
- [engineer-agents.md](./engineer-agents.md) - Padrões de implementação
- [CHANGELOG.md](../../CHANGELOG.md) - Histórico de versões