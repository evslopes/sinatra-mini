# Story 1.1: Otimização Estrutural — Índices Compostos

## Contexto
Como Arquiteto, quero aplicar índices compostos na tabela de `Pedidos` para garantir que filtros por loja e data sejam executados de forma eficiente (O(log N)), melhorando o desempenho de todas as consultas de relatório.

## Épico
Épico 1: Otimização Estrutural (Foundation)

## Tarefas Técnicas
- [x] Entidade: adicionar `@Index` composto na tabela `PEDIDO` para `(loja_id, data_pedido)`
- [x] Validação: verificar criação do índice no H2 via `INFORMATION_SCHEMA.INDEXES`
- [x] Performance: confirmar uso do índice em queries com `EXPLAIN PLAN`
- [x] Documentação: registrar decisão técnica no README

## Critérios de Aceitação

✅ **Índice Composto Presente**
- Anotação `@Index` configurada na entidade `Pedido`
- Nome do índice: `idx_loja_data`
- Colunas: `loja_id, data_pedido` (nessa ordem)

✅ **Explain Plan Otimizado**
- Consultas com filtro por `loja_id` e `data_pedido` utilizam o índice
- Complexidade de busca: O(log N) em vez de O(N) (full table scan)

✅ **Validação em H2**
- Índice visível em `INFORMATION_SCHEMA.INDEXES`
- Schema DDL gerado pelo Hibernate inclui o índice

## Implementação

### Código Aplicado

```java
@Entity
@Table(name = "PEDIDO", indexes = {
    @Index(name = "idx_loja_data", columnList = "loja_id, data_pedido")
})
public class Pedido {
    // ...campos
}
```

### Justificativa Técnica

**Por que índice composto?**
- Todas as queries de relatório (Stories 2.1-2.4) filtram por `loja_id` E `data_pedido`
- Índice composto `(loja_id, data_pedido)` permite:
  1. Busca rápida por loja (primeira coluna)
  2. Range scan eficiente por data dentro da loja (segunda coluna)
  3. Ordenação natural por data (útil para `ORDER BY`)

**Ordem das colunas**
- `loja_id` vem primeiro por ter cardinalidade menor (multi-tenancy)
- `data_pedido` vem segundo para permitir range queries eficientes

**Impacto**
- Queries de relatório evitam full table scans
- Complexidade reduzida de O(N) para O(log N + K), onde K é o número de registros retornados

## Como Validar Localmente

### 1. Verificar Criação do Índice

Acessar H2 Console em `http://localhost:8080/h2-console`:

```sql
SELECT INDEX_NAME, TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION
FROM INFORMATION_SCHEMA.INDEXES
WHERE TABLE_NAME = 'PEDIDO'
ORDER BY INDEX_NAME, ORDINAL_POSITION;
```

**Resultado esperado:**
```
INDEX_NAME      | TABLE_NAME | COLUMN_NAME   | ORDINAL_POSITION
idx_loja_data   | PEDIDO     | loja_id       | 1
idx_loja_data   | PEDIDO     | data_pedido   | 2
PRIMARY_KEY_*   | PEDIDO     | id            | 1
```

### 2. Verificar Uso do Índice (Explain Plan)

```sql
EXPLAIN SELECT CAST(p.data_pedido AS DATE) AS dia, SUM(p.total) AS total
FROM PEDIDO p
WHERE p.loja_id = 1
GROUP BY CAST(p.data_pedido AS DATE)
ORDER BY dia;
```

**Buscar no plano:**
- `/* PUBLIC.idx_loja_data */` ou similar indicando uso do índice
- Ausência de `/* PUBLIC.PEDIDO: tableScan */` (full scan)

### 3. Teste de Performance Comparativo

**Sem índice (simulação):**
```sql
-- Remover índice temporariamente
DROP INDEX idx_loja_data;

-- Executar query e medir tempo
SELECT COUNT(*) FROM PEDIDO WHERE loja_id = 1 AND data_pedido >= CURRENT_DATE - 30;
```

**Com índice:**
```sql
-- Recriar índice
CREATE INDEX idx_loja_data ON PEDIDO(loja_id, data_pedido);

-- Executar mesma query
SELECT COUNT(*) FROM PEDIDO WHERE loja_id = 1 AND data_pedido >= CURRENT_DATE - 30;
```

**Ganho esperado:** 10-100x mais rápido em datasets grandes (10k+ registros)

## Status & Links

- **Issue**: #7 (Otimização de Performance - Índices)
- **PR**: Incluído no PR principal de otimizações
- **Branch**: `main` (já merged)

## Labels Aplicadas
- `performance`
- `database`
- `foundation`

## Observações

### Multi-Tenancy
- O índice `(loja_id, data_pedido)` é ideal para cenário multi-tenant
- Cada loja acessa apenas seus dados de forma isolada e eficiente

### Índices Adicionais (Futuro)
Se necessário, considerar:
- `idx_pedido_cliente` para queries por cliente
- `idx_pedido_status` para filtros por status
- **Atenção:** Índices em excesso impactam INSERT/UPDATE

### H2 vs Produção
- H2 (in-memory) cria índices automaticamente em desenvolvimento
- Em produção (PostgreSQL/MySQL), índices são críticos para performance
- Validar EXPLAIN PLAN no banco de produção antes do deploy

## Próximos Passos

✅ Story 1.1 completa
➡️ Próxima: Story 2.1 - Relatório CSV (aproveita o índice criado)

## Checklist de Conclusão

- [x] Índice `@Index` aplicado na entidade `Pedido`
- [x] Índice visível no H2 Console
- [x] Explain Plan confirmado (queries usam o índice)
- [x] Documentação atualizada no README.md
- [x] Código revisado e merged na `main`

---

**Critério de "Done":**
✅ Índice presente na entidade
✅ Queries de relatório otimizadas (verificado via EXPLAIN)
✅ Documentação completa
