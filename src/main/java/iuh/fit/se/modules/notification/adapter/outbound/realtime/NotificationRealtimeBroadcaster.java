package iuh.fit.se.modules.notification.adapter.outbound.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.port.out.NotificationRealtimePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Primary
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRealtimeBroadcaster implements NotificationRealtimePort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationRealtimeLocalDispatcher localDispatcher;

    @Override
    public void publish(Long userId, CustomerNotificationResponse notification) {
        publishEnvelope(NotificationRealtimeEnvelope.notification(userId, notification));
    }

    @Override
    public void publishEventToUser(Long userId, RealtimeEventResponse event) {
        publishEnvelope(NotificationRealtimeEnvelope.eventToUser(userId, event));
    }

    @Override
    public void publishEventToUserExceptDevice(Long userId, String excludedDeviceId, RealtimeEventResponse event) {
        publishEnvelope(NotificationRealtimeEnvelope.eventToUserExceptDevice(userId, excludedDeviceId, event));
    }

    @Override
    public void publishEventToRoles(Set<String> roles, RealtimeEventResponse event) {
        publishEnvelope(NotificationRealtimeEnvelope.eventToRoles(roles, event));
    }

    private void publishEnvelope(NotificationRealtimeEnvelope envelope) {
        try {
            String payload = objectMapper.writeValueAsString(envelope);
            Long subscribers = redisTemplate.convertAndSend(NotificationRealtimeRedisConfig.CHANNEL, payload);
            if (subscribers != null && subscribers > 0) {
                return;
            }
            log.warn("No Redis realtime subscribers were available; dispatching locally");
        } catch (Exception exception) {
            log.warn("Could not publish realtime event through Redis; dispatching locally: {}", exception.getMessage());
        }
        localDispatcher.dispatch(envelope);
    }
}
