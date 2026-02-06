-- V2__add_performance_indexes.sql
-- Índices compostos para otimização de queries de relatórios
-- Baseado em 03-OTIMIZACAO-PERFORMANCE.md

-- Índice composto para queries filtradas por loja e data
-- Usado em: daily-sales, weekly-average, longest-no-sales
CREATE INDEX idx_pedido_loja_data ON pedido(loja_id, data_pedido);

-- Índice para busca por número de pedido (queries CRUD)
CREATE INDEX idx_pedido_numero ON pedido(numero_pedido);

-- Índice para busca por status (filtros de relatório)
CREATE INDEX idx_pedido_status ON pedido(status);

-- Índice composto para queries de itens de pedido
CREATE INDEX idx_item_pedido_pedido ON item_pedido(pedido_id);
CREATE INDEX idx_item_pedido_produto ON item_pedido(produto_id);

-- Índice para busca de produtos por SKU (CRUD)
CREATE INDEX idx_produto_sku ON produto(codigo_sku);

-- Índice para busca de lojas por CNPJ (validação única)
CREATE INDEX idx_loja_cnpj ON loja(cnpj);

-- Índice parcial para pedidos ativos (excluindo cancelados)
-- Otimiza queries de relatórios que geralmente ignoram pedidos cancelados
CREATE INDEX idx_pedido_loja_data_ativo ON pedido(loja_id, data_pedido) 
    WHERE status != 'CANCELADO';

-- Índice para ordenação por data (queries temporais)
CREATE INDEX idx_pedido_data_desc ON pedido(data_pedido DESC);

-- Comentários para documentação
COMMENT ON INDEX idx_pedido_loja_data IS 'Índice composto crítico para queries de relatórios por loja e período';
COMMENT ON INDEX idx_pedido_loja_data_ativo IS 'Índice parcial para pedidos não cancelados (otimização de relatórios)';
COMMENT ON INDEX idx_pedido_data_desc IS 'Índice para ordenação descendente (pedidos mais recentes primeiro)';
