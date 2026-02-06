package pro.sinatra.mini.cache;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import pro.sinatra.mini.config.CacheConfig;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.service.PedidoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes = {CacheConfig.class, PedidoService.class, CacheMetricsIntegrationTest.MeterRegistryConfig.class},
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false",
                "management.endpoints.web.exposure.include=prometheus,metrics,health"
        }
)
public class CacheMetricsIntegrationTest {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private PedidoRepository pedidoRepository;

    @TestConfiguration
    static class MeterRegistryConfig {
        @Bean
        public io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public org.springframework.context.ApplicationListener<org.springframework.context.event.ContextRefreshedEvent> bindCaffeineToMeter(
                org.springframework.cache.CacheManager cacheManager,
                io.micrometer.core.instrument.MeterRegistry meterRegistry
        ) {
            return event -> {
                for (String name : cacheManager.getCacheNames()) {
                    org.springframework.cache.Cache c = cacheManager.getCache(name);
                    if (c instanceof org.springframework.cache.caffeine.CaffeineCache) {
                        com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache =
                                (com.github.benmanes.caffeine.cache.Cache<?, ?>) ((org.springframework.cache.caffeine.CaffeineCache) c).getNativeCache();
                        io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics.monitor(meterRegistry, nativeCache, name);
                    }
                }
            };
        }
    }

    @Test
    public void testCacheMetricsIncrementOnCalls() {
        Long lojaId = 99L;

        PedidoRepository.DailySalesProjection proj = new PedidoRepository.DailySalesProjection() {
            @Override
            public LocalDate getDia() { return LocalDate.of(2025,1,1); }
            @Override
            public Double getTotal() { return 100.0; }
        };

        when(pedidoRepository.streamDailySalesByLoja(lojaId)).thenReturn(Stream.of(proj));

        // Warm up/first call -> miss
        var first = pedidoService.getDailySalesCached(lojaId);
        // Second call -> hit
        var second = pedidoService.getDailySalesCached(lojaId);

        assertNotNull(first);
        assertEquals(first, second);

        // Wait briefly to allow metric binders to update (usually immediate)
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        // Assert that Micrometer has at least one cache-related meter with non-zero measurement
        boolean hasCacheMetricWithValue = false;
        for (io.micrometer.core.instrument.Meter m : meterRegistry.getMeters()) {
            if (m.getId().getName().toLowerCase().contains("cache")) {
                for (io.micrometer.core.instrument.Measurement ms : m.measure()) {
                    if (ms.getValue() > 0) {
                        hasCacheMetricWithValue = true;
                        break;
                    }
                }
            }
            if (hasCacheMetricWithValue) break;
        }

        assertTrue(hasCacheMetricWithValue, "expected at least one cache-related meter with value > 0");
    }
}
