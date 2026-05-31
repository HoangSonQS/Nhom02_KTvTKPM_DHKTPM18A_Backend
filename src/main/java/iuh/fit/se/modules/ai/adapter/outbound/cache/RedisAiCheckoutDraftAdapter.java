package iuh.fit.se.modules.ai.adapter.outbound.cache;

import iuh.fit.se.modules.ai.application.port.out.AiCheckoutDraftPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAiCheckoutDraftAdapter implements AiCheckoutDraftPersistencePort {

    private static final String KEY_PREFIX = "ai:checkout-draft:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Optional<CheckoutDraft> findBySessionId(String sessionId) {
        try {
            Object value = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
            return value instanceof CheckoutDraft draft ? Optional.of(draft) : Optional.empty();
        } catch (Exception e) {
            log.warn("Unable to load AI checkout draft for session {}: {}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(String sessionId, CheckoutDraft draft) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, draft, TTL);
        } catch (Exception e) {
            log.warn("Unable to save AI checkout draft for session {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void delete(String sessionId) {
        try {
            redisTemplate.delete(KEY_PREFIX + sessionId);
        } catch (Exception e) {
            log.warn("Unable to delete AI checkout draft for session {}: {}", sessionId, e.getMessage());
        }
    }
}
