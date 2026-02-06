package pro.sinatra.mini.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import pro.sinatra.mini.config.CacheConfig;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.service.MaiorIntervaloSemVendasService;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = {CacheConfig.class, MaiorIntervaloSemVendasService.class},
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false"
        }
)
public class ReportCacheIntegrationTest {

    @Autowired
    private MaiorIntervaloSemVendasService maiorService;

    @MockBean
    private PedidoRepository pedidoRepository;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @Test
    public void testCalcularMaiorIntervalo_IsCached() {
        Long lojaId = 10L;
        LocalDate start = LocalDate.of(2025,1,1);
        LocalDate end = LocalDate.of(2025,1,10);

        PedidoRepository.DailySaleDaysProjection proj = new PedidoRepository.DailySaleDaysProjection() {
            @Override
            public LocalDate getDia() { return LocalDate.of(2025,1,3); }
        };

        when(pedidoRepository.streamDistinctSaleDaysByLoja(lojaId, start, end)).thenReturn(Stream.of(proj));

        var first = maiorService.calcularMaiorIntervalo(lojaId, start, end);
        var second = maiorService.calcularMaiorIntervalo(lojaId, start, end);

        assertNotNull(first);
        assertEquals(first, second);

        verify(pedidoRepository, times(1)).streamDistinctSaleDaysByLoja(lojaId, start, end);

        // Validate Caffeine stats for report cache: 2 requests, 1 miss
        var cache = (org.springframework.cache.caffeine.CaffeineCache) cacheManager.getCache("maiorIntervalo");
        assertNotNull(cache, "maiorIntervalo cache should exist");
        com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
        var stats = nativeCache.stats();
        assertEquals(2L, stats.requestCount(), "expected 2 cache requests");
        assertEquals(1L, stats.missCount(), "expected 1 cache miss");
    }
}
