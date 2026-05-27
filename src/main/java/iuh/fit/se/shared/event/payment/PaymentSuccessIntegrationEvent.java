package iuh.fit.se.shared.event.payment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentSuccessIntegrationEvent — Sự kiện tích hợp khi thanh toán thành công.
 * Sử dụng Record để đảm bảo tính bất biến và dễ sử dụng trong các Module khác.
 */
public record PaymentSuccessIntegrationEvent(
    String id,
    String correlationId,
    Long orderId,
    Long userId,
    String transactionId,
    BigDecimal amount,
    String paymentMethod,
    String orderRequestId,  // Order's requestId = CouponReservation.referenceId
    LocalDateTime occurredAt
) implements Serializable {
    public static PaymentSuccessIntegrationEvent of(Long orderId, Long userId, String transactionId, BigDecimal amount, String paymentMethod, String correlationId, String orderRequestId) {
        return new PaymentSuccessIntegrationEvent(
            java.util.UUID.randomUUID().toString(),
            correlationId,
            orderId,
            userId,
            transactionId,
            amount,
            paymentMethod,
            orderRequestId,
            LocalDateTime.now()
        );
    }
}
