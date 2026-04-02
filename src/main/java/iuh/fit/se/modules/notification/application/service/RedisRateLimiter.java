package iuh.fit.se.modules.notification.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * RedisRateLimiter — Chống gửi trùng lặp email (Spam) trên cùng một đơn hàng trong window 5 phút.
 * Áp dụng Distributed Lock bằng Redis (Staff+ Standard).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * Kiểm tra và đánh dấu rate limit.
     *
     * @param orderId ID đơn hàng
     * @param type    Loại thông báo (ORDER_CREATED, PAYMENT_SUCCESS)
     * @return true nếu được phép gửi, false nếu bị rate limit
     */
    public boolean allowRequest(Long orderId, String type) {
        String key = String.format("notification:rate:%d:%s", orderId, type);
        
        // Sử dụng setIfAbsent (SETNX) để đánh dấu
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "SENT", 5, TimeUnit.MINUTES);
        
        if (Boolean.FALSE.equals(success)) {
            log.warn("Rate limit triggered for order {} type {}. Skipping notification.", orderId, type);
            return false;
        }
        
        return true;
    }
}
