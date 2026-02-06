-- V2__add_performance_indexes.sql
-- Índices compostos para otimização de queries de relatórios
-- Baseado em 03-OTIMIZACAO-PERFORMANCE.md

-- Índice composto para queries filtradas por loja e data
-- Usado em: daily-sales, weekly-average, longest-no-sales
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_pedido_loja_data' AND relkind = 'i') THEN
        CREATE INDEX idx_pedido_loja_data ON pedido(loja_id, data_pedido);
    END IF;
END$$;

-- Índice para busca por número de pedido (queries CRUD)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_pedido_numero' AND relkind = 'i') THEN
        CREATE INDEX idx_pedido_numero ON pedido(numero_pedido);
    END IF;
END$$;

-- Índice para busca por status (filtros de relatório)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_pedido_status' AND relkind = 'i') THEN
        CREATE INDEX idx_pedido_status ON pedido(status);
    END IF;
END$$;

-- Índice composto para queries de itens de pedido
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_item_pedido_pedido' AND relkind = 'i') THEN
        CREATE INDEX idx_item_pedido_pedido ON item_pedido(pedido_id);
    END IF;
END$$;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_item_pedido_produto' AND relkind = 'i') THEN
        CREATE INDEX idx_item_pedido_produto ON item_pedido(produto_id);
    END IF;
END$$;

-- Índice para busca de produtos por SKU (CRUD)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_produto_sku' AND relkind = 'i') THEN
        CREATE INDEX idx_produto_sku ON produto(codigo_sku);
    END IF;
END$$;

-- Índice para busca de lojas por CNPJ (validação única)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_loja_cnpj' AND relkind = 'i') THEN
        CREATE INDEX idx_loja_cnpj ON loja(cnpj);
    END IF;
END$$;

-- Índice parcial para pedidos ativos (excluindo cancelados)
-- Otimiza queries de relatórios que geralmente ignoram pedidos cancelados
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_pedido_loja_data_ativo' AND relkind = 'i') THEN
        CREATE INDEX idx_pedido_loja_data_ativo ON pedido(loja_id, data_pedido) 
            WHERE status != 'CANCELADO';
    END IF;
END$$;

-- Índice para ordenação por data (queries temporais)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_pedido_data_desc' AND relkind = 'i') THEN
        CREATE INDEX idx_pedido_data_desc ON pedido(data_pedido DESC);
    END IF;
END$$;

-- Comentários para documentação
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_pedido_loja_data' AND relkind = 'i') THEN
        COMMENT ON INDEX idx_pedido_loja_data IS 'Índice composto crítico para queries de relatórios por loja e período';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_pedido_loja_data_ativo' AND relkind = 'i') THEN
        COMMENT ON INDEX idx_pedido_loja_data_ativo IS 'Índice parcial para pedidos não cancelados (otimização de relatórios)';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_pedido_data_desc' AND relkind = 'i') THEN
        COMMENT ON INDEX idx_pedido_data_desc IS 'Índice para ordenação descendente (pedidos mais recentes primeiro)';
    END IF;
END$$;
