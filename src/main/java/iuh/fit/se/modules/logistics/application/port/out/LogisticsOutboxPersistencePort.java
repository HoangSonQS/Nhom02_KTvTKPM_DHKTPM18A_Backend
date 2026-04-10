package iuh.fit.se.modules.logistics.application.port.out;

import iuh.fit.se.modules.logistics.domain.LogisticsOutboxEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LogisticsOutboxPersistencePort {
    LogisticsOutboxEvent save(LogisticsOutboxEvent event);
    Optional<LogisticsOutboxEvent> findById(UUID id);
    List<LogisticsOutboxEvent> findPendingEvents();
    void delete(UUID id);
}
