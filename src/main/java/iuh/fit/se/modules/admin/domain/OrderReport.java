package iuh.fit.se.modules.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OrderReport — Read Model cho hệ thống báo cáo Admin (CQRS).
 * Chứa dữ liệu denormalized từ nhiều module để phục vụ truy vấn nhanh.
 */
@Entity
@Table(name = "adm_order_report", indexes = {
        @Index(name = "idx_adm_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_adm_created_at", columnList = "created_at"),
        @Index(name = "idx_adm_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(length = 20)
    private String status; // PENDING_PAYMENT, PAID, CANCELLED

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "items_summary", columnDefinition = "TEXT")
    private String itemsSummary;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    // Timestamps cho ATTP (Average Time to Payment) và Funnel analytics
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "checkout_at")
    private Instant checkoutAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void markPaid(Instant paidAt, String paymentMethod) {
        this.status = "PAID";
        this.paidAt = paidAt;
        this.paymentMethod = paymentMethod;
    }

    public void markCancelled(String reason) {
        this.status = "CANCELLED";
        this.cancellationReason = reason;
    }
}
