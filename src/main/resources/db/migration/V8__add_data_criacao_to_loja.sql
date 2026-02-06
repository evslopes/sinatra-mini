-- V8: add data_criacao to loja
ALTER TABLE IF EXISTS loja
  ADD COLUMN IF NOT EXISTS data_criacao DATE;
