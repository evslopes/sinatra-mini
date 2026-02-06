-- V10: backfill pedido.total from valor_total and set NOT NULL
-- Set null totals to valor_total when available, else 0.0
UPDATE pedido
SET total = COALESCE(valor_total, 0.0)
WHERE total IS NULL;

-- Ensure column is not nullable (do this only if safe; use IF NOT EXISTS pattern via DO)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name='pedido' AND column_name='total' AND is_nullable='YES'
  ) THEN
    ALTER TABLE pedido ALTER COLUMN total SET NOT NULL;
  END IF;
END$$;
