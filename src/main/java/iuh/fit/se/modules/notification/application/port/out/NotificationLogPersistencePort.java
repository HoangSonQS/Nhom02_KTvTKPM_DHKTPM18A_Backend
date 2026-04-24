package iuh.fit.se.modules.notification.application.port.out;

import iuh.fit.se.modules.notification.domain.NotificationLog;
import java.util.List;
import java.util.Optional;

public interface NotificationLogPersistencePort {
    NotificationLog save(NotificationLog notificationLog);
    Optional<NotificationLog> findByEventId(String eventId);
    Optional<NotificationLog> findById(Long id);
    List<NotificationLog> findAll();
}
