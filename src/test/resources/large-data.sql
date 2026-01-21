-- Large data load script for H2 (100k pedidos)
-- Inserts a loja and 100k pedidos using SYSTEM_RANGE

-- create a loja if not exists
INSERT INTO LOJA (NOME, DATA_CRIACAO) VALUES ('Loja Load', CURRENT_DATE);

-- Insert 100k pedidos referencing the loja inserted above
INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID)
SELECT DATEADD('MINUTE', SEQ4(), TIMESTAMP '2020-01-01 00:00:00'), 'OK', 10 + MOD(SEQ4(), 100), 'CARD', (SELECT ID FROM LOJA LIMIT 1), NULL
FROM SYSTEM_RANGE(1, 100000);

-- Note: run with H2 RUNSCRIPT or via JDBC