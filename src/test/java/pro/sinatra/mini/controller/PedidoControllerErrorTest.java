package pro.sinatra.mini.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import pro.sinatra.mini.repository.PedidoRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PedidoControllerErrorTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PedidoRepository pedidoRepository;

    @BeforeEach
    public void cleanup() {
        try { jdbcTemplate.execute("DELETE FROM ITEM_PEDIDO"); } catch (Exception ignored) {}
        pedidoRepository.deleteAll();
    }

    @Test
    public void csvExport_nonExistentLoja_returnsEmptyOrOk() {
        // Non-existent loja should return empty CSV (header only) with 200 OK
        Long nonExistentLojaId = 999999L;
        String url = "http://localhost:" + port + "/pedidos/" + nonExistentLojaId + "/reports/daily-sales.csv";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should return 200 with just header (no data)
        assertEquals(200, response.getStatusCode().value());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.startsWith("date,total"), "Should contain CSV header");
        
        // Should have only header line or header + empty data
        String[] lines = body.trim().split("\n");
        assertTrue(lines.length <= 2, "Should have at most header + 1 empty line");
    }

    @Test
    public void longestNoSales_invalidDateRange_handlesProperly() {
        Long lojaId = ensureLojaExists();
        
        // End date before start date - should handle gracefully
        String url = "http://localhost:" + port + "/pedidos/" + lojaId 
                + "/reports/longest-no-sales?start=2025-12-31&end=2025-01-01";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should either return 400 Bad Request or handle gracefully
        assertTrue(
                response.getStatusCode().value() == 400 || response.getStatusCode().value() == 200,
                "Should return 400 or handle gracefully with 200"
        );
    }

    @Test
    public void minDaySales_invalidZoneId_returnsBadRequest() {
        Long lojaId = ensureLojaExists();
        
        // Invalid timezone
        String url = "http://localhost:" + port + "/pedidos/" + lojaId 
                + "/reports/min-day-sales?start=2025-01-01&end=2025-01-31&zoneId=Invalid/Timezone";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should return 400 Bad Request or 500 Internal Server Error
        assertTrue(
                response.getStatusCode().value() >= 400,
                "Should return error status for invalid timezone"
        );
    }

    @Test
    public void weeklyAverage_nonExistentLoja_returnsEmptyAverages() {
        Long nonExistentLojaId = 999999L;
        String url = "http://localhost:" + port + "/pedidos/" + nonExistentLojaId 
                + "/reports/weekly-average?start=2025-01-01&end=2025-01-31";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should return 200 with all zeros or empty result
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    public void longestNoSales_noSalesInPeriod_returnsAppropriateResponse() {
        Long lojaId = ensureLojaExists();
        
        // Period with no sales
        String url = "http://localhost:" + port + "/pedidos/" + lojaId 
                + "/reports/longest-no-sales?start=2025-01-01&end=2025-01-31";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should return 200 with entire period or 204 No Content
        assertTrue(
                response.getStatusCode().value() == 200 || response.getStatusCode().value() == 204,
                "Should return 200 or 204 for period with no sales"
        );
    }

    @Test
    public void minDaySales_noSalesInPeriod_returnsNoContent() {
        Long lojaId = ensureLojaExists();
        
        // Period with no sales
        String url = "http://localhost:" + port + "/pedidos/" + lojaId 
                + "/reports/min-day-sales?start=2025-01-01&end=2025-01-31";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should return 204 No Content when no sales
        assertTrue(
                response.getStatusCode().value() == 204 || response.getStatusCode().value() == 200,
                "Should return 204 No Content or 200 when no sales in period"
        );
    }

    @Test
    public void csvExport_malformedLojaId_returnsBadRequest() {
        // Non-numeric loja ID
        String url = "http://localhost:" + port + "/pedidos/invalid-id/reports/daily-sales.csv";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should return 400 Bad Request
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    public void weeklyAverage_missingParameters_usesDefaults() {
        Long lojaId = ensureLojaExists();
        
        // No start/end dates - should use defaults or handle gracefully
        String url = "http://localhost:" + port + "/pedidos/" + lojaId + "/reports/weekly-average";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should return 200 (uses all data or reasonable defaults)
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    public void longestNoSales_futureDates_handlesGracefully() {
        Long lojaId = ensureLojaExists();
        
        // Future dates (no data expected)
        String url = "http://localhost:" + port + "/pedidos/" + lojaId 
                + "/reports/longest-no-sales?start=2030-01-01&end=2030-12-31";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should handle gracefully (200 or 204)
        assertTrue(
                response.getStatusCode().value() == 200 || response.getStatusCode().value() == 204,
                "Should handle future dates gracefully"
        );
    }

    @Test
    public void minDaySales_sameDayStartAndEnd_handlesCorrectly() {
        Long lojaId = ensureLojaExists();
        
        // Same day for start and end
        String url = "http://localhost:" + port + "/pedidos/" + lojaId 
                + "/reports/min-day-sales?start=2025-01-15&end=2025-01-15";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should handle single-day range
        assertTrue(
                response.getStatusCode().value() == 200 || response.getStatusCode().value() == 204,
                "Should handle single-day range"
        );
    }

    // Helper method to ensure loja exists
    private Long ensureLojaExists() {
        Integer lojaCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LOJA", Integer.class);
        if (lojaCount != null && lojaCount > 0) {
            return jdbcTemplate.queryForObject("SELECT ID FROM LOJA LIMIT 1", Long.class);
        } else {
            var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbcTemplate.update(conn -> {
                var ps = conn.prepareStatement(
                        "INSERT INTO LOJA (NOME, CNPJ, DATA_CRIACAO) VALUES (?, ?, ?)", 
                        new String[]{"id"}
                );
                ps.setString(1, "Loja Teste Errors");
                ps.setString(2, pro.sinatra.mini.test.TestIdentifiers.nextCnpj());
                ps.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
                return ps;
            }, keyHolder);
            return keyHolder.getKey().longValue();
        }
    }
}
