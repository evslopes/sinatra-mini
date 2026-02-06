-- V9: alter total column type in pedido and ensure cliente_id FK exists
-- Alter type to double precision if needed
ALTER TABLE IF EXISTS pedido
  ALTER COLUMN total TYPE double precision USING total::double precision;

-- Add cliente_id column if missing and FK
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'pedido' AND column_name = 'cliente_id'
  ) THEN
    ALTER TABLE pedido ADD COLUMN cliente_id BIGINT;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name = 'pedido' AND kcu.column_name = 'cliente_id'
  ) THEN
    ALTER TABLE pedido
      ADD CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id);
  END IF;
END$$;
