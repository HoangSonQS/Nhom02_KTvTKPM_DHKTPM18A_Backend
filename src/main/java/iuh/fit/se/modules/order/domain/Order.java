package iuh.fit.se.modules.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ord_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false)
    private SagaStatus sagaStatus;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Setter
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    public void markStockReserved() {
        this.sagaStatus = SagaStatus.STOCK_RESERVED;
    }

    public void markCouponReserved() {
        this.sagaStatus = SagaStatus.COUPON_RESERVED;
    }

    public void markSagaCompleted() {
        this.sagaStatus = SagaStatus.COMPLETED;
    }

    public void markCompensated() {
        this.sagaStatus = SagaStatus.COMPENSATED;
    }

    public void markCompensating() {
        this.sagaStatus = SagaStatus.COMPENSATING;
    }

    public void markFailed() {
        this.sagaStatus = SagaStatus.FAILED;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void markPaid() {
        if (this.status == OrderStatus.PENDING_PAYMENT) {
            this.status = OrderStatus.PAID;
        }
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }
}
