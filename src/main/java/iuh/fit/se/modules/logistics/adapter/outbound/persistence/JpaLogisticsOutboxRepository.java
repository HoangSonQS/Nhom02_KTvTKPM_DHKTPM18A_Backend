package iuh.fit.se.modules.logistics.adapter.outbound.persistence;

import iuh.fit.se.modules.logistics.domain.LogisticsOutboxEvent;
import iuh.fit.se.modules.logistics.domain.LogisticsOutboxStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaLogisticsOutboxRepository extends JpaRepository<LogisticsOutboxEvent, UUID> {
    List<LogisticsOutboxEvent> findByStatus(LogisticsOutboxStatus status);
}
