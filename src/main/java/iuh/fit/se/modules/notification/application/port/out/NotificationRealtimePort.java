package iuh.fit.se.modules.notification.application.port.out;

import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;

import java.util.Set;

public interface NotificationRealtimePort {

    void publish(Long userId, CustomerNotificationResponse notification);

    void publishEventToUser(Long userId, RealtimeEventResponse event);

    void publishEventToUserExceptDevice(Long userId, String excludedDeviceId, RealtimeEventResponse event);

    void publishEventToRoles(Set<String> roles, RealtimeEventResponse event);
}
