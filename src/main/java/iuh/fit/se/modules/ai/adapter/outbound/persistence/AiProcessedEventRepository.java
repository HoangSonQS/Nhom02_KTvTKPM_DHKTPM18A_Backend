package iuh.fit.se.modules.ai.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiProcessedEventRepository extends JpaRepository<AiProcessedEventJpaEntity, UUID> {

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO ai_processed_events (event_id, status, processed_at) VALUES (:eventId, 'PROCESSING', NOW()) ON CONFLICT DO NOTHING", nativeQuery = true)
    int tryLockEvent(@Param("eventId") UUID eventId);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE AiProcessedEventJpaEntity e SET e.status = 'DONE' WHERE e.eventId = :eventId")
    void markAsDone(@Param("eventId") UUID eventId);
}
