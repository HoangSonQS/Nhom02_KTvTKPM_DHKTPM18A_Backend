package iuh.fit.se.modules.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pay_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "return_request_id")
    private String returnRequestId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void markSuccess(String transactionId, String resultData) {
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.resultData = resultData;
    }

    public void markFailed(String resultData) {
        this.status = PaymentStatus.FAILED;
        this.resultData = resultData;
    }

    public void markRefunded(BigDecimal refundAmount, String returnRequestId) {
        this.status = PaymentStatus.REFUNDED;
        this.refundAmount = refundAmount;
        this.returnRequestId = returnRequestId;
    }
}
