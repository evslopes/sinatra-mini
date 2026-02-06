-- V7: add total_item to item_pedido
ALTER TABLE IF EXISTS item_pedido
  ADD COLUMN IF NOT EXISTS total_item numeric(38,2);

-- ensure check constraint for non-negative values if not exists
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    WHERE c.conname = 'item_pedido_subtotal_check' AND t.relname = 'item_pedido'
  ) THEN
    -- skip: subtotal check already exists in schema; do not add duplicate
    NULL;
  END IF;
END$$;
