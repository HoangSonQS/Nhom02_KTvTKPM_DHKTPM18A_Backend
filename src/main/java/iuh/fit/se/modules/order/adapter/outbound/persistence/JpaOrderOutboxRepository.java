package iuh.fit.se.modules.order.adapter.outbound.persistence;

import iuh.fit.se.modules.order.domain.OrderOutboxEvent;
import iuh.fit.se.modules.order.domain.OrderOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaOrderOutboxRepository extends JpaRepository<OrderOutboxEvent, String> {
    List<OrderOutboxEvent> findByStatus(OrderOutboxStatus status);
}
