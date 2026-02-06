-- V4: add columns that were applied manually during debugging
ALTER TABLE IF EXISTS pedido_item
    ADD COLUMN IF NOT EXISTS descricao_produto VARCHAR(1024);

ALTER TABLE IF EXISTS pedido_item
    ADD COLUMN IF NOT EXISTS total_item NUMERIC(19,2);

ALTER TABLE IF EXISTS pedido
    ADD COLUMN IF NOT EXISTS metodo_pagamento VARCHAR(100);

ALTER TABLE IF EXISTS pedido
    ADD COLUMN IF NOT EXISTS total NUMERIC(19,2);

-- Ensure data_criacao is DATE (convert if necessary)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='pedido' AND column_name='data_criacao') THEN
        BEGIN
            ALTER TABLE pedido ALTER COLUMN data_criacao TYPE DATE USING (data_criacao::date);
        EXCEPTION WHEN undefined_function OR invalid_text_representation THEN
            -- conversion failed; leave column as-is for manual inspection
            RAISE NOTICE 'Skipping data_criacao type conversion: conversion failed or not applicable.';
        END;
    END IF;
END$$;
