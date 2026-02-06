-- V1__initial_schema.sql
-- Criação do schema inicial para PostgreSQL
-- Migração da estrutura existente em H2 para PostgreSQL

-- Tabela: loja
CREATE TABLE IF NOT EXISTS loja (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cnpj VARCHAR(18) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela: produto
CREATE TABLE IF NOT EXISTS produto (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    codigo_sku VARCHAR(50) UNIQUE NOT NULL,
    preco_unitario DECIMAL(10, 2) NOT NULL CHECK (preco_unitario >= 0),
    categoria VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela: pedido
CREATE TABLE IF NOT EXISTS pedido (
    id BIGSERIAL PRIMARY KEY,
    numero_pedido VARCHAR(50) UNIQUE NOT NULL,
    loja_id BIGINT NOT NULL,
    data_pedido TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    valor_total DECIMAL(12, 2) NOT NULL CHECK (valor_total >= 0),
    observacao TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pedido_loja FOREIGN KEY (loja_id) REFERENCES loja(id) ON DELETE CASCADE
);

-- Tabela: item_pedido
CREATE TABLE IF NOT EXISTS item_pedido (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario DECIMAL(10, 2) NOT NULL CHECK (preco_unitario >= 0),
    subtotal DECIMAL(12, 2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_pedido_produto FOREIGN KEY (produto_id) REFERENCES produto(id) ON DELETE RESTRICT
);

-- Comentários para documentação
COMMENT ON TABLE loja IS 'Lojas cadastradas no sistema multistore';
COMMENT ON TABLE produto IS 'Catálogo de produtos disponíveis';
COMMENT ON TABLE pedido IS 'Pedidos realizados por loja';
COMMENT ON TABLE item_pedido IS 'Itens de cada pedido (relação N:M entre pedido e produto)';

COMMENT ON COLUMN pedido.status IS 'Status do pedido: PENDENTE, PROCESSANDO, CONCLUIDO, CANCELADO';
COMMENT ON COLUMN pedido.data_pedido IS 'Data e hora do pedido (sem fuso horário - UTC armazenado)';
