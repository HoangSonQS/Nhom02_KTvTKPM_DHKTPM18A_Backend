package iuh.fit.se.modules.ai.adapter.outbound.cache;

import iuh.fit.se.modules.ai.application.port.out.AiBookContextPersistencePort.BookContext;
import iuh.fit.se.modules.ai.application.port.out.AiBookContextPersistencePort.BookReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAiBookContextAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisAiBookContextAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisAiBookContextAdapter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void whenSaveAndLoadContext_thenUseSessionKeyAndThirtyMinuteTtl() {
        BookContext context = new BookContext(List.of(new BookReference(10L, "Clean Code")));
        when(valueOperations.get("ai:book-context:session-1")).thenReturn(context);

        adapter.save("session-1", context);

        assertThat(adapter.findBySessionId("session-1")).contains(context);
        verify(valueOperations).set("ai:book-context:session-1", context, Duration.ofMinutes(30));
    }

    @Test
    void whenRedisReadFails_thenReturnEmptyContext() {
        when(valueOperations.get("ai:book-context:session-1")).thenThrow(new IllegalStateException("Redis unavailable"));

        assertThatCode(() -> adapter.findBySessionId("session-1")).doesNotThrowAnyException();
        assertThat(adapter.findBySessionId("session-1")).isEmpty();
    }
}
