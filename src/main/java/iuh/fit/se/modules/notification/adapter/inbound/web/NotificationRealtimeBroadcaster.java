package iuh.fit.se.modules.notification.adapter.inbound.web;

import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.port.out.NotificationRealtimePort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Set;

@Primary
@Component
@RequiredArgsConstructor
public class NotificationRealtimeBroadcaster implements NotificationRealtimePort {

    private final NotificationSseAdapter sseAdapter;
    private final NotificationWebSocketAdapter webSocketAdapter;

    @Override
    public void publish(Long userId, CustomerNotificationResponse notification) {
        sseAdapter.publish(userId, notification);
        webSocketAdapter.publish(userId, notification);
    }

    @Override
    public void publishEventToUser(Long userId, RealtimeEventResponse event) {
        sseAdapter.publishEventToUser(userId, event);
        webSocketAdapter.publishEventToUser(userId, event);
    }

    @Override
    public void publishEventToUserExceptDevice(Long userId, String excludedDeviceId, RealtimeEventResponse event) {
        sseAdapter.publishEventToUserExceptDevice(userId, excludedDeviceId, event);
        webSocketAdapter.publishEventToUserExceptDevice(userId, excludedDeviceId, event);
    }

    @Override
    public void publishEventToRoles(Set<String> roles, RealtimeEventResponse event) {
        sseAdapter.publishEventToRoles(roles, event);
        webSocketAdapter.publishEventToRoles(roles, event);
    }
}
