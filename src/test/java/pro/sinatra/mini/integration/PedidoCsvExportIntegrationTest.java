package pro.sinatra.mini.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import pro.sinatra.mini.entity.Loja;
import pro.sinatra.mini.entity.Pedido;
import pro.sinatra.mini.repository.LojaRepository;
import pro.sinatra.mini.repository.PedidoRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PedidoCsvExportIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    

    @BeforeEach
    public void cleanup() {
        // remove dependent rows first (ITEM_PEDIDO) to avoid FK constraint on delete
        try {
            jdbcTemplate.execute("DELETE FROM ITEM_PEDIDO");
        } catch (Exception e) {
            // ignore if table not present or delete fails
        }
        // Only clear pedidos; do not delete all lojas (may be referenced by other test data)
        pedidoRepository.deleteAll();
    }

    @Test
    public void exportDailySalesCsv_generatesCsvWithDailyTotals() throws Exception {
        // Use existing loja if present, otherwise insert one
        Integer lojaCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LOJA", Integer.class);
        long lojaId;
        if (lojaCount != null && lojaCount > 0) {
            lojaId = jdbcTemplate.queryForObject("SELECT ID FROM LOJA LIMIT 1", Long.class);
        } else {
            org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                java.sql.PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO LOJA (NOME, CNPJ, DATA_CRIACAO) VALUES (?, ?, ?)",
                        new String[]{"id"});
                ps.setString(1, "Loja Teste");
                ps.setString(2, pro.sinatra.mini.test.TestIdentifiers.nextCnpj());
                ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                return ps;
            }, keyHolder);
            Number lojaIdNum = keyHolder.getKey();
            assertNotNull(lojaIdNum, "Loja ID should be generated");
            lojaId = lojaIdNum.longValue();
        }

        // generate orders across 10 days, 50 orders per day = 500 entries
        int days = 10;
        int perDay = 50;
        Map<LocalDate, Double> expected = new TreeMap<>();

        for (int d = 0; d < days; d++) {
            LocalDate date = LocalDate.now().minusDays(days - d);
            double dayTotal = 0.0;
            List<Object[]> batchArgs = new ArrayList<>();
            for (int i = 0; i < perDay; i++) {
                LocalDateTime dt = LocalDateTime.of(date, LocalTime.NOON).plusMinutes(i);
                double value = 10.0 + i; // deterministic
                dayTotal += value;
                batchArgs.add(new Object[]{pro.sinatra.mini.test.TestIdentifiers.nextNumeroPedido(), java.sql.Timestamp.valueOf(dt), "OK", value, "CARD", lojaId, null});
            }
            // Insert batch for this day (include NUMERO_PEDIDO)
            jdbcTemplate.batchUpdate("INSERT INTO PEDIDO (NUMERO_PEDIDO, DATA_PEDIDO, STATUS, valor_total, METODO_PAGAMENTO, LOJA_ID, CLIENTE_ID) VALUES (?, ?, ?, ?, ?, ?, ?)", batchArgs);
            expected.put(date, dayTotal);
        }

        String url = "http://localhost:" + port + "/pedidos/" + lojaId + "/reports/daily-sales.csv";
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);

        assertEquals(200, resp.getStatusCode().value());
        // headers: content-disposition and content-type
        assertTrue(resp.getHeaders().containsKey(HttpHeaders.CONTENT_DISPOSITION), "Content-Disposition must be present");
        String cd = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(cd);
        assertTrue(cd.contains("daily-sales-loja-"), "Content-Disposition should contain the filename");
        assertNotNull(resp.getHeaders().getContentType(), "Content-Type must be set");
        assertTrue(resp.getHeaders().getContentType().toString().startsWith("text/csv"));
        String body = resp.getBody();
        assertNotNull(body, "CSV body should not be null");
        String[] lines = body.split("\r?\n");
        assertTrue(lines.length >= days + 1, "CSV should contain header + one line per day");
        assertEquals("date,total", lines[0].trim());

        // parse CSV lines into map and compare with expected
        Map<LocalDate, Double> parsed = Arrays.stream(lines)
                .skip(1)
                .filter(s -> s.trim().length() > 0)
                .map(s -> s.split(","))
                .filter(arr -> arr.length >= 2)
                .collect(Collectors.toMap(
                        arr -> LocalDate.parse(arr[0].trim()),
                        arr -> Double.parseDouble(arr[1].trim()),
                        Double::sum,
                        TreeMap::new
                ));

        // Validate that all expected dates exist and totals match (approx)
        for (Map.Entry<LocalDate, Double> e : expected.entrySet()) {
            assertTrue(parsed.containsKey(e.getKey()), "CSV must contain date: " + e.getKey());
            assertEquals(e.getValue(), parsed.get(e.getKey()), 0.01, "Totals should match for date " + e.getKey());
        }
    }
}
