package iuh.fit.se.modules.notification.application.port.in;

import java.util.List;

public interface NotificationCustomerUseCase {
    List<CustomerNotificationResponse> getMyNotifications(Long userId);
    long countUnread(Long userId);
    void markAsRead(Long userId, Long notificationId);
    void markAllAsRead(Long userId);
}
