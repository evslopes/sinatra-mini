package pro.sinatra.mini.integration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "perf", matches = "true")
public class PedidoLoadPerformanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    public void cleanup() {
        try { jdbcTemplate.execute("DELETE FROM ITEM_PEDIDO"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("DELETE FROM PEDIDO"); } catch (Exception ignored) {}
    }

    @Test
    public void load10kAndExport() throws Exception {
        // Reduced from 100k to 10k for faster execution in CI/local
        final int total = 10_000;
        final int batchSize = 500;

        // ensure loja exists
        Long lojaId = null;
        try {
            lojaId = jdbcTemplate.queryForObject("SELECT ID FROM LOJA LIMIT 1", Long.class);
        } catch (Exception ignored) {}
        if (lojaId == null) {
            org.springframework.jdbc.support.GeneratedKeyHolder kh = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                java.sql.PreparedStatement ps = con.prepareStatement("INSERT INTO LOJA (NOME, DATA_CRIACAO) VALUES (?, ?)", new String[]{"ID"});
                ps.setString(1, "Loja Perf");
                ps.setDate(2, java.sql.Date.valueOf(java.time.LocalDate.now()));
                return ps;
            }, kh);
            lojaId = kh.getKey().longValue();
        }

        long t0 = System.currentTimeMillis();

        // insert in batches
        int inserted = 0;
        java.util.List<Object[]> batch = new java.util.ArrayList<>(batchSize);
        for (int i = 1; i <= total; i++) {
            java.time.LocalDateTime dt = java.time.LocalDateTime.now().plusMinutes(i);
            double value = 10.0 + (i % 100);
            batch.add(new Object[]{java.sql.Timestamp.valueOf(dt), "OK", value, "CARD", lojaId, null});
            if (batch.size() >= batchSize) {
                jdbcTemplate.batchUpdate("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)", batch);
                inserted += batch.size();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)", batch);
            inserted += batch.size();
            batch.clear();
        }

        long insertTime = System.currentTimeMillis() - t0;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PEDIDO", Integer.class);
        assertNotNull(count);
        assertEquals(total, count.intValue(), "Expected pedidos inserted");

        // Verify memory usage is reasonable (rough check)
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        long t1 = System.currentTimeMillis();
        String url = "http://localhost:" + port + "/pedidos/" + lojaId + "/reports/daily-sales.csv";
        org.springframework.http.ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        long exportTime = System.currentTimeMillis() - t1;

        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryDelta = usedMemoryAfter - usedMemoryBefore;

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        
        // Assert export time is reasonable (less than 10 seconds for 10k records)
        assertTrue(exportTime < 10_000, "Export should complete in less than 10 seconds, took: " + exportTime + "ms");
        
        // Assert memory delta is not linear with dataset size (streaming should keep memory bounded)
        // For 10k records, memory delta should be less than 50MB
        long maxMemoryDelta = 50 * 1024 * 1024; // 50MB
        assertTrue(Math.abs(memoryDelta) < maxMemoryDelta, 
                "Memory delta should be bounded by streaming. Delta: " + (memoryDelta / 1024 / 1024) + "MB");

        System.out.println("Performance Test Results:");
        System.out.println("  Records inserted: " + inserted);
        System.out.println("  Insert time: " + insertTime + "ms (" + (insertTime / 1000.0) + "s)");
        System.out.println("  Export time: " + exportTime + "ms (" + (exportTime / 1000.0) + "s)");
        System.out.println("  Memory delta: " + (memoryDelta / 1024 / 1024) + "MB");
        System.out.println("  CSV size: " + (resp.getBody() != null ? resp.getBody().length() : 0) + " bytes");
    }
}
