package iuh.fit.se.modules.inventory.adapter.outbound.persistence;

import iuh.fit.se.modules.inventory.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
