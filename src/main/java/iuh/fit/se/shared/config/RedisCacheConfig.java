package iuh.fit.se.shared.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Cấu hình mặc định
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Cấu hình riêng cho inventory_stock_cache với TTL ngắn (3 giây) 
        // để hỗ trợ Soft Check cực nhanh mà không bị rủi ro dữ liệu cũ quá lâu
        RedisCacheConfiguration inventoryConfig = defaultConfig.entryTtl(Duration.ofSeconds(3));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(60))) // Default 60 mins
                .withCacheConfiguration("inventory_stock_cache", inventoryConfig)
                .build();
    }
}
