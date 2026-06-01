package iuh.fit.se.modules.notification.adapter.inbound.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iuh.fit.se.modules.notification.adapter.outbound.realtime.NotificationRealtimeBroadcaster;
import iuh.fit.se.modules.notification.adapter.outbound.realtime.NotificationRealtimeLocalDispatcher;
import iuh.fit.se.modules.notification.adapter.outbound.realtime.NotificationRealtimeRedisConfig;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRealtimeBroadcasterTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final NotificationRealtimeLocalDispatcher localDispatcher = mock(NotificationRealtimeLocalDispatcher.class);
    private NotificationRealtimeBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        broadcaster = new NotificationRealtimeBroadcaster(redisTemplate, objectMapper, localDispatcher);
    }

    @Test
    void whenRedisHasSubscribers_thenDoNotDispatchLocally() {
        when(redisTemplate.convertAndSend(eq(NotificationRealtimeRedisConfig.CHANNEL), any(String.class)))
                .thenReturn(2L);

        broadcaster.publishEventToRoles(Set.of("ADMIN"), event());

        verify(redisTemplate).convertAndSend(eq(NotificationRealtimeRedisConfig.CHANNEL), any(String.class));
        verify(localDispatcher, never()).dispatch(any());
    }

    @Test
    void whenRedisPublishFails_thenDispatchLocallyOnce() {
        when(redisTemplate.convertAndSend(eq(NotificationRealtimeRedisConfig.CHANNEL), any(String.class)))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        broadcaster.publishEventToRoles(Set.of("ADMIN"), event());

        verify(localDispatcher).dispatch(any());
    }

    private RealtimeEventResponse event() {
        return new RealtimeEventResponse(
                "AUDIT_LOG_CHANGED",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Audit log changed",
                LocalDateTime.now()
        );
    }
}
