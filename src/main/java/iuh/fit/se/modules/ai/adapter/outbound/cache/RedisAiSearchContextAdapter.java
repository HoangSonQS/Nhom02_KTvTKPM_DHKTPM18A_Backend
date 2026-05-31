package iuh.fit.se.modules.ai.adapter.outbound.cache;

import iuh.fit.se.modules.ai.application.port.out.AiSearchContextPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAiSearchContextAdapter implements AiSearchContextPersistencePort {

    private static final String KEY_PREFIX = "ai:search-context:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Optional<SearchContext> findBySessionId(String sessionId) {
        try {
            Object value = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
            return value instanceof SearchContext context ? Optional.of(context) : Optional.empty();
        } catch (Exception e) {
            log.warn("Unable to load AI search context for session {}: {}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(String sessionId, SearchContext context) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, context, TTL);
        } catch (Exception e) {
            log.warn("Unable to save AI search context for session {}: {}", sessionId, e.getMessage());
        }
    }
}
