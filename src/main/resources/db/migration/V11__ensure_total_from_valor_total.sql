-- V11: Ensure pedido.total is populated from valor_total for new rows
-- Idempotent: updates existing NULLs and creates/updates a trigger

-- Backfill existing rows where total IS NULL (no-op if none)
UPDATE pedido
SET total = COALESCE(valor_total, 0.0)
WHERE total IS NULL;

-- Create or replace function used by trigger
CREATE OR REPLACE FUNCTION ensure_pedido_total()
RETURNS trigger AS $$
BEGIN
  IF NEW.total IS NULL THEN
    NEW.total := COALESCE(NEW.valor_total, 0.0);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop existing trigger if present, then create it (idempotent)
DROP TRIGGER IF EXISTS trg_ensure_pedido_total ON pedido;
CREATE TRIGGER trg_ensure_pedido_total
BEFORE INSERT OR UPDATE ON pedido
FOR EACH ROW
EXECUTE FUNCTION ensure_pedido_total();
