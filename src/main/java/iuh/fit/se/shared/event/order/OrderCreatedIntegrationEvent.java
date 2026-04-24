package iuh.fit.se.shared.event.order;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderCreatedIntegrationEvent — Sự kiện tích hợp khi đơn hàng được tạo.
 * Sử dụng Record để đảm bảo tính bất biến và dễ sử dụng trong các Module khác.
 */
public record OrderCreatedIntegrationEvent(
    String id,
    String correlationId,
    Long orderId,
    Long userId,
    String customerName,
    String customerEmail,
    BigDecimal totalAmount,
    String itemsSummary,
    LocalDateTime occurredAt
) implements Serializable {
    public static OrderCreatedIntegrationEvent of(Long orderId, Long userId, String customerName, String customerEmail, BigDecimal totalAmount, String itemsSummary, String correlationId) {
        return new OrderCreatedIntegrationEvent(
            java.util.UUID.randomUUID().toString(),
            correlationId,
            orderId,
            userId,
            customerName,
            customerEmail,
            totalAmount,
            itemsSummary,
            LocalDateTime.now()
        );
    }
}
