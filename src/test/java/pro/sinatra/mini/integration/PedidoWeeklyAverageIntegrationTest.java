package pro.sinatra.mini.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import pro.sinatra.mini.reports.MediaSemanalDTO;
import pro.sinatra.mini.repository.PedidoRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PedidoWeeklyAverageIntegrationTest {

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
    public void weeklyAverage_returnsSevenDays() {
        // Ensure loja exists
        Integer lojaCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LOJA", Integer.class);
        long lojaId;
        if (lojaCount != null && lojaCount > 0) {
            lojaId = jdbcTemplate.queryForObject("SELECT ID FROM LOJA LIMIT 1", Long.class);
        } else {
            var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbcTemplate.update(conn -> {
                var ps = conn.prepareStatement("INSERT INTO LOJA (NOME, DATA_CRIACAO) VALUES (?, ?)", new String[]{"ID"});
                ps.setString(1, "Loja Teste Weekly");
                ps.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
                return ps;
            }, keyHolder);
            lojaId = keyHolder.getKey().longValue();
        }

        // Insert sales for different days of the week
        // January 2025: 1st is Wednesday, 6th is Monday, 7th is Tuesday, etc.
        LocalDate mon1 = LocalDate.of(2025, 1, 6);   // Monday
        LocalDate mon2 = LocalDate.of(2025, 1, 13);  // Monday
        LocalDate tue = LocalDate.of(2025, 1, 7);    // Tuesday
        LocalDate wed = LocalDate.of(2025, 1, 1);    // Wednesday

        // Monday: 100 + 200 = avg 150
        jdbcTemplate.update("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.valueOf(LocalDateTime.of(mon1, LocalTime.NOON)), "OK", 100.0, "CARD", lojaId, null);
        jdbcTemplate.update("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.valueOf(LocalDateTime.of(mon2, LocalTime.NOON)), "OK", 200.0, "CARD", lojaId, null);

        // Tuesday: 300
        jdbcTemplate.update("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.valueOf(LocalDateTime.of(tue, LocalTime.NOON)), "OK", 300.0, "CARD", lojaId, null);

        // Wednesday: 400
        jdbcTemplate.update("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.valueOf(LocalDateTime.of(wed, LocalTime.NOON)), "OK", 400.0, "CARD", lojaId, null);

        String url = "http://localhost:" + port + "/pedidos/" + lojaId + "/reports/weekly-average?start=2025-01-01&end=2025-01-31&zoneId=UTC";
        
        ResponseEntity<List<MediaSemanalDTO>> resp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MediaSemanalDTO>>() {}
        );

        assertEquals(200, resp.getStatusCode().value());
        List<MediaSemanalDTO> result = resp.getBody();
        assertNotNull(result);
        
        // Should return 7 days (Monday to Sunday)
        assertEquals(7, result.size());

        // Verify Monday (day 1) has average 150
        MediaSemanalDTO monday = result.stream()
                .filter(dto -> dto.dayOfWeek() == DayOfWeek.MONDAY.getValue())
                .findFirst()
                .orElse(null);
        assertNotNull(monday);
        assertEquals(150.0, monday.average(), 0.01);

        // Verify Tuesday (day 2) has average 300
        MediaSemanalDTO tuesday = result.stream()
                .filter(dto -> dto.dayOfWeek() == DayOfWeek.TUESDAY.getValue())
                .findFirst()
                .orElse(null);
        assertNotNull(tuesday);
        assertEquals(300.0, tuesday.average(), 0.01);

        // Verify Wednesday (day 3) has average 400
        MediaSemanalDTO wednesday = result.stream()
                .filter(dto -> dto.dayOfWeek() == DayOfWeek.WEDNESDAY.getValue())
                .findFirst()
                .orElse(null);
        assertNotNull(wednesday);
        assertEquals(400.0, wednesday.average(), 0.01);

        // Verify days without sales have 0.0
        MediaSemanalDTO thursday = result.stream()
                .filter(dto -> dto.dayOfWeek() == DayOfWeek.THURSDAY.getValue())
                .findFirst()
                .orElse(null);
        assertNotNull(thursday);
        assertEquals(0.0, thursday.average(), 0.01);
    }

    @Test
    public void weeklyAverage_emptyPeriod_returnsAllZeros() {
        Integer lojaCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LOJA", Integer.class);
        long lojaId;
        if (lojaCount != null && lojaCount > 0) {
            lojaId = jdbcTemplate.queryForObject("SELECT ID FROM LOJA LIMIT 1", Long.class);
        } else {
            var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbcTemplate.update(conn -> {
                var ps = conn.prepareStatement("INSERT INTO LOJA (NOME, DATA_CRIACAO) VALUES (?, ?)", new String[]{"ID"});
                ps.setString(1, "Loja Empty");
                ps.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
                return ps;
            }, keyHolder);
            lojaId = keyHolder.getKey().longValue();
        }

        String url = "http://localhost:" + port + "/pedidos/" + lojaId + "/reports/weekly-average?start=2025-01-01&end=2025-01-31&zoneId=UTC";
        
        ResponseEntity<List<MediaSemanalDTO>> resp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MediaSemanalDTO>>() {}
        );

        assertEquals(200, resp.getStatusCode().value());
        List<MediaSemanalDTO> result = resp.getBody();
        assertNotNull(result);
        assertEquals(7, result.size());

        // All days should have 0.0 average
        result.forEach(dto -> assertEquals(0.0, dto.average(), 0.01));
    }
}
