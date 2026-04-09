package iuh.fit.se.modules.returns.adapter.outbound.persistence;

import iuh.fit.se.modules.returns.application.port.out.ReturnOutboxPersistencePort;
import iuh.fit.se.modules.returns.domain.ReturnOutboxEvent;
import iuh.fit.se.modules.returns.domain.ReturnOutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReturnOutboxPersistenceAdapter implements ReturnOutboxPersistencePort {

    private final JpaReturnOutboxRepository jpaRepository;

    @Override
    public ReturnOutboxEvent save(ReturnOutboxEvent event) {
        return jpaRepository.save(event);
    }

    @Override
    public Optional<ReturnOutboxEvent> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ReturnOutboxEvent> findPendingEvents() {
        return jpaRepository.findByStatus(ReturnOutboxStatus.PENDING);
    }

    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }
}
