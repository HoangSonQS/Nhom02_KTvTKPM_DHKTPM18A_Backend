package iuh.fit.se.modules.logistics.adapter.outbound.persistence;

import iuh.fit.se.modules.logistics.domain.OutboxEvent;
import iuh.fit.se.modules.logistics.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaOutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatus(OutboxStatus status);
}
