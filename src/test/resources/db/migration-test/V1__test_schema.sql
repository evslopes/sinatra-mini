-- V1__test_schema.sql
-- Schema mínimo compatível com H2 para testes locais (fallback)

CREATE TABLE IF NOT EXISTS loja (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(100) NOT NULL,
  cnpj VARCHAR(18) UNIQUE NOT NULL,
  data_criacao DATE,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS produto (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(200) NOT NULL,
  codigo_sku VARCHAR(50) UNIQUE NOT NULL,
  preco_unitario DECIMAL(10,2) NOT NULL,
  categoria VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pedido (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  loja_id BIGINT,
  data_pedido TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
  metodo_pagamento VARCHAR(100),
  total DOUBLE PRECISION NOT NULL,
  cliente_id BIGINT,
  observacao CLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pedido_loja FOREIGN KEY (loja_id) REFERENCES loja(id)
);

-- Composite index required by tests: loja_id + data_pedido
CREATE INDEX IF NOT EXISTS idx_pedido_loja_data ON pedido(loja_id, data_pedido);

CREATE TABLE IF NOT EXISTS item_pedido (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pedido_id BIGINT,
  produto_id BIGINT,
  quantidade INT NOT NULL,
  preco_unitario DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(12,2) DEFAULT 0.00,
  descricao_produto VARCHAR(1024),
  total_item DECIMAL(19,2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id),
  CONSTRAINT fk_item_pedido_produto FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE TABLE IF NOT EXISTS cliente (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  loja_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_cliente_loja FOREIGN KEY (loja_id) REFERENCES loja(id)
);
