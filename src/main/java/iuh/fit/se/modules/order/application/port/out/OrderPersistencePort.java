package iuh.fit.se.modules.order.application.port.out;

import iuh.fit.se.modules.order.domain.FulfillmentStatus;
import iuh.fit.se.modules.order.domain.Order;
import java.math.BigDecimal;
import java.util.Optional;

public interface OrderPersistencePort {
    Order save(Order order);
    Optional<Order> findById(Long id);
    Optional<Order> findByRequestId(String requestId);
    java.util.List<Order> findByUserId(Long userId);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    
    java.util.List<Order> findAbandonedOrders(iuh.fit.se.modules.order.domain.SagaStatus excludeStatus, java.time.LocalDateTime before, int limit);
    
    java.util.List<Order> findAll();

    java.util.List<Order> searchAdminOrders(FulfillmentStatus status);

    java.util.List<TopSellingBookProjection> findTopSellingBooks(java.util.List<FulfillmentStatus> statuses, int limit);

    java.util.List<TopSellingBookProjection> findBookSales(
            java.util.List<FulfillmentStatus> statuses,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to);
    
    boolean updateSagaStatusAtomic(Long orderId, iuh.fit.se.modules.order.domain.SagaStatus currentStatus, iuh.fit.se.modules.order.domain.SagaStatus nextStatus);

    record TopSellingBookProjection(
            Long bookId,
            String title,
            long quantitySold,
            BigDecimal revenue
    ) {}
}
