package iuh.fit.se.modules.returns.application.port.out;

import iuh.fit.se.modules.returns.domain.ReturnOutboxEvent;

import java.util.List;
import java.util.Optional;

public interface ReturnOutboxPersistencePort {
    ReturnOutboxEvent save(ReturnOutboxEvent event);
    Optional<ReturnOutboxEvent> findById(String id);
    List<ReturnOutboxEvent> findPendingEvents();
    void delete(String id);
}
