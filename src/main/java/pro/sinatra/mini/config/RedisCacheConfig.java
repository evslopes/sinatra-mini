package pro.sinatra.mini.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Value("${sinatra.cache.redis.enabled:false}")
    private boolean redisCacheEnabled;

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        return manager;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager) {
        List<CacheManager> managers = new ArrayList<>();
        // L1: In-memory Caffeine cache (< 0.1ms)
        managers.add(caffeineCacheManager);

        // L2: Distributed Redis cache (enabled in dev/prod profiles)
        if (redisCacheEnabled && redisConnectionFactory != null) {
            RedisCacheConfiguration redisConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .disableCachingNullValues()
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new GenericJackson2JsonRedisSerializer()));

            RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(redisConfig)
                    .transactionAware()
                    .build();

            managers.add(redisCacheManager);
        }

        CompositeCacheManager composite = new CompositeCacheManager();
        composite.setCacheManagers(managers);
        composite.setFallbackToNoOpCache(false);
        return composite;
    }
}
