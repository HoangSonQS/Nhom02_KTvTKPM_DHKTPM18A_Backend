package iuh.fit.se.modules.notification.adapter.outbound.persistence;

import iuh.fit.se.modules.notification.application.port.out.NotificationLogPersistencePort;
import iuh.fit.se.modules.notification.domain.NotificationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationLogPersistencePort {

    private final NotificationLogRepository notificationLogRepository;

    @Override
    public NotificationLog save(NotificationLog notificationLog) {
        return notificationLogRepository.save(notificationLog);
    }

    @Override
    public Optional<NotificationLog> findByEventId(String eventId) {
        return notificationLogRepository.findByEventId(eventId);
    }

    @Override
    public Optional<NotificationLog> findById(Long id) {
        return notificationLogRepository.findById(id);
    }

    @Override
    public List<NotificationLog> findAll() {
        return notificationLogRepository.findAll();
    }

    @Override
    public List<NotificationLog> findByRecipientUserId(Long recipientUserId) {
        return notificationLogRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId);
    }

    @Override
    public long countUnreadByRecipientUserId(Long recipientUserId) {
        return notificationLogRepository.countByRecipientUserIdAndReadAtIsNull(recipientUserId);
    }
}
