package iuh.fit.se.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình Caching cho hệ thống - Phase 9.
 * Hỗ trợ Per-cache TTL và Redis-based distributed cache.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.ttl.catalog-books:600000}")
    private long catalogBooksTtl;

    @Value("${cache.ttl.admin-dashboard:3600000}")
    private long adminDashboardTtl;

    @Value("${cache.ttl.auth-permissions:86400000}")
    private long authPermissionsTtl;

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Cấu hình mặc định: dùng String cho Key (dễ đọc trong Redis), JSON cho Value
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Cấu hình TTL riêng cho từng vùng cache (SLA-based)
        Map<String, RedisCacheConfiguration> cacheNamesConfiguration = new HashMap<>();
        
        cacheNamesConfiguration.put("books", defaultCacheConfig.entryTtl(Duration.ofMillis(catalogBooksTtl)));
        cacheNamesConfiguration.put("bookDetails", defaultCacheConfig.entryTtl(Duration.ofMillis(catalogBooksTtl)));
        cacheNamesConfiguration.put("dashboardStats", defaultCacheConfig.entryTtl(Duration.ofMillis(adminDashboardTtl)));
        cacheNamesConfiguration.put("userPermissions", defaultCacheConfig.entryTtl(Duration.ofMillis(authPermissionsTtl)));

        // Gộp từ RedisCacheConfig cũ: inventory_stock_cache với TTL ngắn (3 giây)
        cacheNamesConfiguration.put("inventory_stock_cache", defaultCacheConfig.entryTtl(Duration.ofSeconds(3)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheNamesConfiguration)
                .transactionAware()
                .build();
    }
}
