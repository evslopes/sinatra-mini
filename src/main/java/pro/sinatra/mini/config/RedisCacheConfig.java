package pro.sinatra.mini.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisCacheConfig {

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager) {
        List<CacheManager> managers = new ArrayList<>();

        if (redisConnectionFactory != null) {
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

        managers.add(caffeineCacheManager);

        CompositeCacheManager composite = new CompositeCacheManager();
        composite.setCacheManagers(managers);
        composite.setFallbackToNoOpCache(false);

        return composite;
    }
}
