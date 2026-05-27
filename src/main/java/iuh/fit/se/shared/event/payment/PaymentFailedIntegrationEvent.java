package iuh.fit.se.shared.event.payment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedIntegrationEvent(
        String id,
        String correlationId,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String paymentMethod,
        String responseCode,
        LocalDateTime occurredAt
) implements Serializable {
    public static PaymentFailedIntegrationEvent of(
            Long orderId,
            Long userId,
            BigDecimal amount,
            String paymentMethod,
            String responseCode,
            String correlationId
    ) {
        return new PaymentFailedIntegrationEvent(
                UUID.randomUUID().toString(),
                correlationId,
                orderId,
                userId,
                amount,
                paymentMethod,
                responseCode,
                LocalDateTime.now()
        );
    }
}
