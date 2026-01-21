package pro.sinatra.mini.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import pro.sinatra.mini.reports.MenorVendaDiaDTO;
import pro.sinatra.mini.repository.PedidoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PedidoMinDaySalesIntegrationTest {

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
    public void minDaySales_integration() {
        // ensure loja exists
        Integer lojaCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LOJA", Integer.class);
        long lojaId;
        if (lojaCount != null && lojaCount > 0) {
            lojaId = jdbcTemplate.queryForObject("SELECT ID FROM LOJA LIMIT 1", Long.class);
        } else {
            var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbcTemplate.update(conn -> {
                var ps = conn.prepareStatement("INSERT INTO LOJA (NOME, DATA_CRIACAO) VALUES (?, ?)", new String[]{"ID"});
                ps.setString(1, "Loja Teste");
                ps.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
                return ps;
            }, keyHolder);
            lojaId = keyHolder.getKey().longValue();
        }

        // Insert sales totals across three days: day1=100, day2=70, day3=200
        LocalDate d1 = LocalDate.of(2025,1,1);
        LocalDate d2 = LocalDate.of(2025,1,2);
        LocalDate d3 = LocalDate.of(2025,1,3);

        // day1: one order 100
        jdbcTemplate.update("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.valueOf(LocalDateTime.of(d1, LocalTime.NOON)), "OK", 100.0, "CARD", lojaId, null);

        // day2: two orders 50 and 20 = 70
        jdbcTemplate.update("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.valueOf(LocalDateTime.of(d2, LocalTime.NOON)), "OK", 50.0, "CARD", lojaId, null);
        jdbcTemplate.update("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.valueOf(LocalDateTime.of(d2, LocalTime.NOON.plusHours(3))), "OK", 20.0, "CARD", lojaId, null);

        // day3: one order 200
        jdbcTemplate.update("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.valueOf(LocalDateTime.of(d3, LocalTime.NOON)), "OK", 200.0, "CARD", lojaId, null);

        String url = "http://localhost:" + port + "/pedidos/" + lojaId + "/reports/min-day-sales?start=2025-01-01&end=2025-01-03&zoneId=UTC";
        ResponseEntity<MenorVendaDiaDTO> resp = restTemplate.getForEntity(url, MenorVendaDiaDTO.class);
        assertEquals(200, resp.getStatusCode().value());
        MenorVendaDiaDTO dto = resp.getBody();
        assertNotNull(dto);
        assertEquals(d2, dto.dia());
        assertEquals(70.0, dto.total(), 0.01);
    }
}
