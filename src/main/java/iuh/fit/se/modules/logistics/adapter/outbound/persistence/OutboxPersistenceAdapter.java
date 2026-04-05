package iuh.fit.se.modules.logistics.adapter.outbound.persistence;

import iuh.fit.se.modules.logistics.application.port.out.OutboxPersistencePort;
import iuh.fit.se.modules.logistics.domain.OutboxEvent;
import iuh.fit.se.modules.logistics.domain.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxPersistencePort {
    private final JpaOutboxRepository repository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return repository.save(event);
    }

    @Override
    public Optional<OutboxEvent> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<OutboxEvent> findPendingEvents() {
        return repository.findByStatus(OutboxStatus.PENDING);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
