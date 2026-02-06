-- V3__seed_test_data.sql
-- Dados de teste para validação em ambientes dev/staging
-- NÃO será executado em produção (via profiles Flyway)

-- Inserir lojas de teste
INSERT INTO loja (nome, cnpj) VALUES 
    ('Loja Centro', '12.345.678/0001-90'),
    ('Loja Shopping', '98.765.432/0001-10'),
    ('Loja Outlet', '11.222.333/0001-44')
ON CONFLICT (cnpj) DO NOTHING;

-- Inserir produtos de teste
INSERT INTO produto (nome, codigo_sku, preco_unitario, categoria) VALUES 
    ('Notebook Dell Inspiron 15', 'DELL-INS15-001', 3499.99, 'INFORMATICA'),
    ('Mouse Logitech MX Master 3', 'LOGI-MX3-001', 459.90, 'PERIFERICOS'),
    ('Teclado Mecânico Keychron K2', 'KEYCH-K2-001', 699.00, 'PERIFERICOS'),
    ('Monitor LG 27" 4K', 'LG-27UK-001', 1899.00, 'INFORMATICA'),
    ('Webcam Logitech C920', 'LOGI-C920-001', 379.90, 'PERIFERICOS'),
    ('Headset HyperX Cloud II', 'HYPERX-C2-001', 549.00, 'AUDIO'),
    ('SSD Samsung 1TB', 'SAMS-SSD1T-001', 799.00, 'ARMAZENAMENTO'),
    ('Memória RAM 16GB DDR4', 'CRUCIAL-16GB-001', 459.00, 'HARDWARE'),
    ('Placa de Vídeo RTX 3060', 'NVIDIA-3060-001', 2499.00, 'HARDWARE'),
    ('Gabinete NZXT H510', 'NZXT-H510-001', 599.00, 'GABINETE')
ON CONFLICT (codigo_sku) DO NOTHING;

-- Inserir pedidos de teste para Loja Centro (últimos 30 dias)
-- Pedidos com diferentes padrões para testar algoritmos de relatórios
DO $$
DECLARE
    loja_centro_id BIGINT;
    produto_ids BIGINT[];
    base_date TIMESTAMP;
    i INTEGER;
BEGIN
    -- Obter ID da Loja Centro
    SELECT id INTO loja_centro_id FROM loja WHERE cnpj = '12.345.678/0001-90';
    
    -- Obter IDs de produtos
    SELECT ARRAY_AGG(id) INTO produto_ids FROM produto LIMIT 10;
    
    -- Data base: 30 dias atrás
    base_date := CURRENT_TIMESTAMP - INTERVAL '30 days';
    
    -- Inserir 50 pedidos com padrões variados (idempotente)
    FOR i IN 1..50 LOOP
        DECLARE
            pedido_num TEXT := 'PED-CENTRO-' || LPAD(i::TEXT, 5, '0');
            exists_pedido BOOLEAN;
            new_pedido_id BIGINT;
        BEGIN
            SELECT EXISTS (SELECT 1 FROM pedido WHERE numero_pedido = pedido_num) INTO exists_pedido;
            IF NOT exists_pedido THEN
                INSERT INTO pedido (numero_pedido, loja_id, data_pedido, status, valor_total)
                VALUES (
                    pedido_num,
                    loja_centro_id,
                    base_date + (i || ' days')::INTERVAL + (FLOOR(RANDOM() * 12) || ' hours')::INTERVAL,
                    CASE 
                        WHEN i % 10 = 0 THEN 'CANCELADO'
                        WHEN i % 5 = 0 THEN 'PROCESSANDO'
                        ELSE 'CONCLUIDO'
                    END,
                    ROUND((RANDOM() * 5000 + 500)::NUMERIC, 2)
                ) RETURNING id INTO new_pedido_id;

                -- Inserir 2-5 itens por pedido
                INSERT INTO item_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal)
                SELECT 
                    new_pedido_id,
                    produto_ids[FLOOR(RANDOM() * ARRAY_LENGTH(produto_ids, 1) + 1)],
                    FLOOR(RANDOM() * 5 + 1)::INTEGER,
                    ROUND((RANDOM() * 1000 + 100)::NUMERIC, 2),
                    ROUND((RANDOM() * 5000 + 100)::NUMERIC, 2)
                FROM generate_series(1, FLOOR(RANDOM() * 4 + 2)::INTEGER);
            END IF;
        END;
    END LOOP;
END $$;

-- Comentário para alertar sobre execução em produção
COMMENT ON TABLE loja IS 'ATENÇÃO: V3__seed_test_data.sql NÃO deve ser executado em produção!';
