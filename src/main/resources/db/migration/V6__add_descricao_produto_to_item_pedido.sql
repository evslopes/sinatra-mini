-- V6: add descricao_produto to item_pedido
ALTER TABLE IF EXISTS item_pedido
  ADD COLUMN IF NOT EXISTS descricao_produto VARCHAR(1024);
