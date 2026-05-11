package iuh.fit.se.modules.order.application.port.out;

import iuh.fit.se.modules.order.domain.Order;
import java.util.Optional;

public interface OrderPersistencePort {
    Order save(Order order);
    Optional<Order> findById(Long id);
    Optional<Order> findByRequestId(String requestId);
    java.util.List<Order> findByUserId(Long userId);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    
    java.util.List<Order> findAbandonedOrders(iuh.fit.se.modules.order.domain.SagaStatus excludeStatus, java.time.LocalDateTime before, int limit);
    
    java.util.List<Order> findAll();
    
    boolean updateSagaStatusAtomic(Long orderId, iuh.fit.se.modules.order.domain.SagaStatus currentStatus, iuh.fit.se.modules.order.domain.SagaStatus nextStatus);
}
