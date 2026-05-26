package iuh.fit.se.shared.event.order;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusChangedIntegrationEvent(
        String id,
        String correlationId,
        Long orderId,
        Long userId,
        String customerName,
        String customerEmail,
        String fromStatus,
        String toStatus,
        String reason,
        LocalDateTime occurredAt
) implements Serializable {
    public static OrderStatusChangedIntegrationEvent of(
            Long orderId,
            Long userId,
            String customerName,
            String customerEmail,
            String fromStatus,
            String toStatus,
            String reason,
            String correlationId
    ) {
        return new OrderStatusChangedIntegrationEvent(
                UUID.randomUUID().toString(),
                correlationId,
                orderId,
                userId,
                customerName,
                customerEmail,
                fromStatus,
                toStatus,
                reason,
                LocalDateTime.now()
        );
    }
}
