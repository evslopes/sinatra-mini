package pro.sinatra.mini.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import pro.sinatra.mini.reports.PeriodoDTO;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.repository.LojaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PedidoLongestNoSalesIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private LojaRepository lojaRepository;

    @BeforeEach
    public void cleanup() {
        try { jdbcTemplate.execute("DELETE FROM ITEM_PEDIDO"); } catch (Exception ignored) {}
        pedidoRepository.deleteAll();
    }

    @Test
    public void longestNoSales_integration() {
        // ensure loja
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

        // Insert sales on specific dates
        LocalDate start = LocalDate.of(2025,1,1);
        LocalDate end = LocalDate.of(2025,1,15);
        LocalDate[] sales = new LocalDate[] {
                LocalDate.of(2025,1,2),
                LocalDate.of(2025,1,3),
                LocalDate.of(2025,1,7),
                LocalDate.of(2025,1,14)
        };

        for (LocalDate d : sales) {
            LocalDateTime dt = LocalDateTime.of(d, LocalTime.NOON);
            jdbcTemplate.update("INSERT INTO PEDIDO (DATA_PEDIDO, STATUS, TOTAL, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?)",
                    java.sql.Timestamp.valueOf(dt), "OK", 10.0, "CARD", lojaId, null);
        }

        String url = "http://localhost:" + port + "/pedidos/" + lojaId + "/reports/longest-no-sales?start=2025-01-01&end=2025-01-15";
        ResponseEntity<PeriodoDTO> resp = restTemplate.getForEntity(url, PeriodoDTO.class);
        assertEquals(200, resp.getStatusCodeValue());
        PeriodoDTO dto = resp.getBody();
        assertNotNull(dto);
        assertEquals(LocalDate.of(2025,1,8), dto.dataInicial());
        assertEquals(LocalDate.of(2025,1,13), dto.dataFinal());
        assertEquals(6, dto.dias());
    }
}
