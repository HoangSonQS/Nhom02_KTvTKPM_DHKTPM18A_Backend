package iuh.fit.se.modules.admin.adapter.outbound.persistence;

import iuh.fit.se.modules.admin.domain.OrderReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderReportRepository extends JpaRepository<OrderReport, Long> {
    
    Optional<OrderReport> findByOrderId(Long orderId);

    /**
     * Atomic UPSERT với State Guard để tránh lùi trạng thái (State Downgrade).
     * Cho phép cập nhật từ 'PENDING_PAYMENT' sang trạng thái mới (CONFIRMED, CANCELLED).
     */
    @Modifying
    @Query(value = "UPDATE adm_order_report SET status = :status, paid_at = :paidAt, payment_method = :paymentMethod " +
            "WHERE order_id = :orderId AND status = 'PENDING_PAYMENT'", nativeQuery = true)
    int updateStatusToPaidAtomic(@Param("orderId") Long orderId, 
                                 @Param("status") String status, 
                                 @Param("paidAt") Instant paidAt, 
                                 @Param("paymentMethod") String paymentMethod);

    // Dashboard Metrics
    // "Đã thanh toán" = mọi đơn hàng đã qua bước confirm payment (CONFIRMED, PROCESSING, DELIVERING, DELIVERED)
    @Query("SELECT COUNT(o) FROM OrderReport o WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'DELIVERING', 'DELIVERED')")
    long countPaidOrders();

    @Query(value = """
        SELECT COALESCE(
            AVG(
                EXTRACT(EPOCH FROM o.paid_at) - EXTRACT(EPOCH FROM o.checkout_at)
            ), 
            0.0
        )
        FROM adm_order_report o 
        WHERE o.paid_at IS NOT NULL 
          AND o.checkout_at IS NOT NULL
        """, nativeQuery = true)
    double calculateAverageTimeToPaymentSeconds();

    @Query("SELECT AVG(o.totalAmount) FROM OrderReport o WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'DELIVERING', 'DELIVERED')")
    BigDecimal calculateAverageOrderValue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderReport o WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'DELIVERING', 'DELIVERED')")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.refundAmount), 0) FROM OrderReport o WHERE o.refundAmount IS NOT NULL")
    BigDecimal calculateRefundAmount();

    @Query("SELECT COUNT(DISTINCT o.customerName) FROM OrderReport o WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'DELIVERING', 'DELIVERED')")
    long countUniqueBuyers();

    @Query("SELECT o FROM OrderReport o WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'DELIVERING', 'DELIVERED')")
    List<OrderReport> findPaidReports();
}
