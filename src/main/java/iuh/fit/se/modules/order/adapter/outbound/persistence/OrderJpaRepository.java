package iuh.fit.se.modules.order.adapter.outbound.persistence;

import iuh.fit.se.modules.order.domain.Order;
import iuh.fit.se.modules.order.domain.SagaStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByRequestId(String requestId);

    @Query("SELECT o FROM Order o WHERE o.sagaStatus NOT IN (:statuses) AND o.createdAt < :before")
    List<Order> findAbandonedOrders(@Param("statuses") List<SagaStatus> statuses, 
                                     @Param("before") LocalDateTime before, 
                                     Pageable pageable);

    @Modifying
    @Query("UPDATE Order o SET o.sagaStatus = :nextStatus WHERE o.id = :id AND o.sagaStatus = :currentStatus")
    int updateSagaStatusAtomic(@Param("id") Long id, 
                               @Param("currentStatus") SagaStatus currentStatus, 
                               @Param("nextStatus") SagaStatus nextStatus);
}
