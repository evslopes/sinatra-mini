-- V5: create cliente table
CREATE TABLE IF NOT EXISTS cliente (
  id BIGSERIAL PRIMARY KEY,
  nome VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  loja_id BIGINT,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cliente_loja ON cliente(loja_id);

-- FK to loja (safe if already exists)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name = 'cliente' AND kcu.column_name = 'loja_id'
  ) THEN
    ALTER TABLE cliente
      ADD CONSTRAINT fk_cliente_loja FOREIGN KEY (loja_id) REFERENCES loja(id) ON DELETE SET NULL;
  END IF;
END$$;
-- V5__create_cliente_table.sql
-- Cria tabela cliente compatível com a entidade `Cliente`

CREATE TABLE IF NOT EXISTS cliente (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    loja_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cliente_loja FOREIGN KEY (loja_id) REFERENCES loja(id) ON DELETE SET NULL
);

COMMENT ON TABLE cliente IS 'Clientes cadastrados; relação com loja (opcional)';
