package iuh.fit.se.modules.logistics.application.port.out;

import iuh.fit.se.modules.logistics.domain.OutboxEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxPersistencePort {
    OutboxEvent save(OutboxEvent event);
    Optional<OutboxEvent> findById(UUID id);
    List<OutboxEvent> findPendingEvents();
    void delete(UUID id);
}
