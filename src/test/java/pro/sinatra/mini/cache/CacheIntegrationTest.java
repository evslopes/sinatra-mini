package pro.sinatra.mini.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import pro.sinatra.mini.config.CacheConfig;
import pro.sinatra.mini.repository.LojaRepository;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.service.LojaService;
import pro.sinatra.mini.service.PedidoService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import static org.mockito.Mockito.*;

@SpringBootTest(
    classes = {CacheConfig.class, LojaService.class, PedidoService.class},
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false"
    }
)
public class CacheIntegrationTest {

    @Autowired
    private LojaService lojaService;

    @Autowired
    private PedidoService pedidoService;

    @MockBean
    private LojaRepository lojaRepository;

    @MockBean
    private PedidoRepository pedidoRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testLojaFindById_IsCached() {
        Long id = 1L;
        // return a simple mock Loja instance
        pro.sinatra.mini.entity.Loja lojaMock = mock(pro.sinatra.mini.entity.Loja.class);
        when(lojaRepository.findById(id)).thenReturn(Optional.of(lojaMock));

        // first call should invoke repository
        var first = lojaService.findById(id);
        var second = lojaService.findById(id);

        assertNotNull(first);
        assertSame(first, second);

        verify(lojaRepository, times(1)).findById(id);
    }

    @Test
    public void testGetDailySalesCached_Hit() {
        Long lojaId = 2L;
        PedidoRepository.DailySalesProjection proj = new PedidoRepository.DailySalesProjection() {
            @Override
            public LocalDate getDia() {
                return LocalDate.of(2025,1,1);
            }

            @Override
            public Double getTotal() {
                return 42.5;
            }
        };

        when(pedidoRepository.streamDailySalesByLoja(lojaId)).thenReturn(Stream.of(proj));

        var first = pedidoService.getDailySalesCached(lojaId);
        var second = pedidoService.getDailySalesCached(lojaId);

        assertNotNull(first);
        assertEquals(1, first.size());
        assertEquals(first, second);

        verify(pedidoRepository, times(1)).streamDailySalesByLoja(lojaId);

        // Validate Caffeine stats: 2 requests, 1 miss
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache("relatorios");
        assertNotNull(cache, "relatorios cache should exist");
        com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache.getNativeCache();
        var stats = nativeCache.stats();
        assertEquals(2L, stats.requestCount(), "expected 2 cache requests");
        assertEquals(1L, stats.missCount(), "expected 1 cache miss");
    }
}
