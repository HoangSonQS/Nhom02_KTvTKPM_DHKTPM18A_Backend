package iuh.fit.se.modules.returns.adapter.outbound.persistence;

import iuh.fit.se.modules.returns.domain.ReturnOutboxEvent;
import iuh.fit.se.modules.returns.domain.ReturnOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaReturnOutboxRepository extends JpaRepository<ReturnOutboxEvent, String> {
    List<ReturnOutboxEvent> findByStatus(ReturnOutboxStatus status);
}
