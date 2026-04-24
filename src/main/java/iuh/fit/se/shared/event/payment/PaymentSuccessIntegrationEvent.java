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
    String transactionId,
    BigDecimal amount,
    String paymentMethod,
    LocalDateTime occurredAt
) implements Serializable {
    public static PaymentSuccessIntegrationEvent of(Long orderId, String transactionId, BigDecimal amount, String paymentMethod, String correlationId) {
        return new PaymentSuccessIntegrationEvent(
            java.util.UUID.randomUUID().toString(),
            correlationId,
            orderId,
            transactionId,
            amount,
            paymentMethod,
            LocalDateTime.now()
        );
    }
}
