package iuh.fit.se.modules.notification.adapter.outbound.persistence;

import iuh.fit.se.modules.notification.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    Optional<NotificationLog> findByEventId(String eventId);
}
