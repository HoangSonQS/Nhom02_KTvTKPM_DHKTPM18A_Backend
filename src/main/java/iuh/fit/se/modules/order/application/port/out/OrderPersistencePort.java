package iuh.fit.se.modules.order.application.port.out;

import iuh.fit.se.modules.order.domain.Order;
import java.util.Optional;

public interface OrderPersistencePort {
    void save(Order order);
    Optional<Order> findById(Long id);
    Optional<Order> findByRequestId(String requestId);
    
    java.util.List<Order> findAbandonedOrders(iuh.fit.se.modules.order.domain.SagaStatus excludeStatus, java.time.LocalDateTime before, int limit);
    
    boolean updateSagaStatusAtomic(Long orderId, iuh.fit.se.modules.order.domain.SagaStatus currentStatus, iuh.fit.se.modules.order.domain.SagaStatus nextStatus);
}
