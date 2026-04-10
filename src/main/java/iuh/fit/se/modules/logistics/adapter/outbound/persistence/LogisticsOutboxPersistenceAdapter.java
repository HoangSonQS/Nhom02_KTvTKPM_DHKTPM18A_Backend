package iuh.fit.se.modules.logistics.adapter.outbound.persistence;

import iuh.fit.se.modules.logistics.application.port.out.LogisticsOutboxPersistencePort;
import iuh.fit.se.modules.logistics.domain.LogisticsOutboxEvent;
import iuh.fit.se.modules.logistics.domain.LogisticsOutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LogisticsOutboxPersistenceAdapter implements LogisticsOutboxPersistencePort {
    private final JpaLogisticsOutboxRepository repository;

    @Override
    public LogisticsOutboxEvent save(LogisticsOutboxEvent event) {
        return repository.save(event);
    }

    @Override
    public Optional<LogisticsOutboxEvent> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<LogisticsOutboxEvent> findPendingEvents() {
        return repository.findByStatus(LogisticsOutboxStatus.PENDING);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
