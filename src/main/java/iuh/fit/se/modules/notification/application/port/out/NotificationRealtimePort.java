package iuh.fit.se.modules.notification.application.port.out;

import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;

public interface NotificationRealtimePort {

    void publish(Long userId, CustomerNotificationResponse notification);
}
