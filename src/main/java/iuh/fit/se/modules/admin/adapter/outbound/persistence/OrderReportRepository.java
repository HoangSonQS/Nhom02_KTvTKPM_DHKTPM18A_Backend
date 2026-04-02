package iuh.fit.se.modules.admin.adapter.outbound.persistence;

import iuh.fit.se.modules.admin.domain.OrderReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface OrderReportRepository extends JpaRepository<OrderReport, Long> {
    
    Optional<OrderReport> findByOrderId(Long orderId);

    /**
     * Atomic UPSERT với State Guard để tránh lùi trạng thái (State Downgrade).
     * Chỉ cho phép cập nhật trạng thái từ 'PENDING_PAYMENT' sang 'PAID' hoặc 'CANCELLED'.
     */
    @Modifying
    @Query(value = "UPDATE adm_order_report SET status = :status, paid_at = :paidAt, payment_method = :paymentMethod " +
            "WHERE order_id = :orderId AND status = 'PENDING_PAYMENT'", nativeQuery = true)
    int updateStatusToPaidAtomic(@Param("orderId") Long orderId, 
                                 @Param("status") String status, 
                                 @Param("paidAt") Instant paidAt, 
                                 @Param("paymentMethod") String paymentMethod);

    // Dashboard Metrics
    @Query("SELECT COUNT(o) FROM OrderReport o WHERE o.status = 'PAID'")
    long countPaidOrders();

    @Query("""
        SELECT COALESCE(
            AVG(
                EXTRACT(EPOCH FROM o.paidAt) - EXTRACT(EPOCH FROM o.checkoutAt)
            ), 
            0.0
        )
        FROM OrderReport o 
        WHERE o.paidAt IS NOT NULL 
          AND o.checkoutAt IS NOT NULL
        """)
    Double calculateAverageTimeToPaymentSeconds();

    @Query("SELECT AVG(o.totalAmount) FROM OrderReport o WHERE o.status = 'PAID'")
    BigDecimal calculateAverageOrderValue();

    @Query("SELECT COUNT(DISTINCT o.customerName) FROM OrderReport o WHERE o.status = 'PAID'")
    long countUniqueBuyers();
}
