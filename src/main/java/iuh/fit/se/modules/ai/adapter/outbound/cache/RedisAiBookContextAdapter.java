package iuh.fit.se.modules.ai.adapter.outbound.cache;

import iuh.fit.se.modules.ai.application.port.out.AiBookContextPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAiBookContextAdapter implements AiBookContextPersistencePort {

    private static final String KEY_PREFIX = "ai:book-context:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Optional<BookContext> findBySessionId(String sessionId) {
        try {
            Object value = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
            return value instanceof BookContext context ? Optional.of(context) : Optional.empty();
        } catch (Exception e) {
            log.warn("Unable to load AI book context for session {}: {}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(String sessionId, BookContext context) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, context, TTL);
        } catch (Exception e) {
            log.warn("Unable to save AI book context for session {}: {}", sessionId, e.getMessage());
        }
    }
}
