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

    /**
     * Trạng thái hoàn thành đơn hàng. Source of truth duy nhất kể từ V27.
     *
     * <p>Luồng chuẩn: PENDING → CONFIRMED → PROCESSING → DELIVERING → DELIVERED
     * <p>Nhánh hủy: CONFIRMED / PROCESSING / DELIVERING → CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", nullable = false)
    private FulfillmentStatus fulfillmentStatus;

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

    // =========================================================
    // Saga State Machine Methods
    // =========================================================

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

    // =========================================================
    // FulfillmentStatus Domain Methods
    // =========================================================

    /**
     * Xác nhận đơn hàng sau khi thanh toán thành công.
     * Trigger: PaymentSuccessIntegrationEvent.
     */
    public void confirm() {
        if (this.fulfillmentStatus == FulfillmentStatus.PENDING) {
            this.fulfillmentStatus = FulfillmentStatus.CONFIRMED;
        }
    }

    /**
     * Chuyển sang trạng thái đang xử lý (đóng gói).
     * Trigger: Admin/Staff action qua isValidAdminTransition().
     */
    public void startProcessing() {
        if (this.fulfillmentStatus == FulfillmentStatus.CONFIRMED) {
            this.fulfillmentStatus = FulfillmentStatus.PROCESSING;
        }
    }

    /**
     * Chuyển sang trạng thái đang giao hàng.
     * Trigger: Admin/Staff action qua isValidAdminTransition().
     */
    public void startDelivering() {
        if (this.fulfillmentStatus == FulfillmentStatus.PROCESSING) {
            this.fulfillmentStatus = FulfillmentStatus.DELIVERING;
        }
    }

    /**
     * Đánh dấu đã giao hàng thành công (terminal positive state).
     * Trigger: Admin/Staff action qua isValidAdminTransition().
     */
    public void markDelivered() {
        if (this.fulfillmentStatus == FulfillmentStatus.DELIVERING) {
            this.fulfillmentStatus = FulfillmentStatus.DELIVERED;
        }
    }

    /**
     * Hủy đơn hàng theo luồng admin transition chuẩn.
     * Áp dụng cho: CONFIRMED, PROCESSING, DELIVERING → CANCELLED.
     *
     * <p>Lưu ý: Không dùng method này cho PENDING. Xem {@link #forceCancel(String)}.
     */
    public void cancelByTransition() {
        this.fulfillmentStatus = FulfillmentStatus.CANCELLED;
    }

    /**
     * Hủy đơn hàng bắt buộc — bỏ qua luồng admin transition thông thường.
     * Dùng cho: hệ thống tự cancel (payment timeout), admin override với lý do rõ ràng.
     *
     * @param reason Lý do bắt buộc phải ghi rõ — dùng cho audit log.
     */
    public void forceCancel(String reason) {
        // reason được caller dùng để ghi audit log — không lưu trong entity
        // để tránh làm nặng domain model với concern của audit layer.
        this.fulfillmentStatus = FulfillmentStatus.CANCELLED;
    }

    /**
     * Reset để thử lại checkout saga.
     */
    public void resetForRetry() {
        this.fulfillmentStatus = FulfillmentStatus.PENDING;
        this.sagaStatus = SagaStatus.INIT;
        this.discountAmount = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDetails(BigDecimal totalAmount, String shippingAddress, String customerPhone) {
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.customerPhone = customerPhone;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

    // =========================================================
    // Admin Transition Guard
    // =========================================================

    /**
     * Kiểm tra tính hợp lệ của admin/staff transition theo luồng operational.
     *
     * <p>Luồng hợp lệ:
     * <pre>
     *   CONFIRMED  → PROCESSING  (xác nhận xử lý)
     *   CONFIRMED  → CANCELLED   (hủy trước khi đóng gói)
     *   PROCESSING → DELIVERING  (bắt đầu giao hàng)
     *   PROCESSING → CANCELLED   (hủy trong quá trình xử lý)
     *   DELIVERING → DELIVERED   (giao hàng thành công)
     *   DELIVERING → PROCESSING  (giao hàng thất bại — quay lại xử lý)
     * </pre>
     *
     * <p>PENDING → CANCELLED KHÔNG nằm ở đây. Dùng {@link #forceCancel(String)} thay thế.
     */
    public static boolean isValidAdminTransition(FulfillmentStatus from, FulfillmentStatus to) {
        return switch (from) {
            case CONFIRMED  -> to == FulfillmentStatus.PROCESSING || to == FulfillmentStatus.CANCELLED;
            case PROCESSING -> to == FulfillmentStatus.DELIVERING || to == FulfillmentStatus.CANCELLED;
            case DELIVERING -> to == FulfillmentStatus.DELIVERED  || to == FulfillmentStatus.PROCESSING;
            default         -> false;
        };
    }
}
