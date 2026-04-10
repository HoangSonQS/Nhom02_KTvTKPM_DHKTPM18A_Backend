package iuh.fit.se.modules.auth.adapter.outbound.persistence;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import iuh.fit.se.modules.auth.application.port.out.OtpPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOtpPersistenceAdapter implements OtpPersistencePort {

    private final StringRedisTemplate redisTemplate;
    private static final String OTP_KEY_PREFIX = "otp:reset:";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:otp:";

    // Local fallback for rate limiting (Resilience Step)
    private final Cache<String, Integer> localRateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Override
    public void saveOtpHash(String email, String otpHash, long expirySeconds) {
        try {
            redisTemplate.opsForValue().set(OTP_KEY_PREFIX + email, otpHash, Duration.ofSeconds(expirySeconds));
        } catch (Exception e) {
            log.error("Failed to save OTP to Redis for {}: {}", email, e.getMessage());
            // Note: OTP must be in Redis to be verified across instances. 
            // If Redis is down, we follow the Plan: OTP Redis Fail -> Reject (Fail-safe).
            throw e;
        }
    }

    @Override
    public Optional<String> getOtpHash(String email) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(OTP_KEY_PREFIX + email));
        } catch (Exception e) {
            log.error("Failed to get OTP from Redis for {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void deleteOtp(String email) {
        try {
            redisTemplate.delete(OTP_KEY_PREFIX + email);
        } catch (Exception e) {
            log.warn("Failed to delete OTP from Redis for {}: {}", email, e.getMessage());
        }
    }

    @Override
    public boolean isAllowedToRequest(String email) {
        String key = RATE_LIMIT_PREFIX + email;
        try {
            String count = redisTemplate.opsForValue().get(key);
            if (count != null && Integer.parseInt(count) >= 3) {
                return false;
            }
        } catch (Exception e) {
            log.warn("Redis rate limit check failed for {}, falling back to Caffeine local cache", email);
            Integer localCount = localRateLimitCache.getIfPresent(email);
            return localCount == null || localCount < 3;
        }
        return true;
    }

    @Override
    public void recordRequest(String email) {
        String key = RATE_LIMIT_PREFIX + email;
        try {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("Redis record request failed for {}, updating local Caffeine cache", email);
            Integer current = localRateLimitCache.getIfPresent(email);
            localRateLimitCache.put(email, (current == null ? 0 : current) + 1);
        }
    }
}
