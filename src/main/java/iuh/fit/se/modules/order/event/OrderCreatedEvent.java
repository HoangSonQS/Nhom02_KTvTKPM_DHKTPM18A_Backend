package iuh.fit.se.modules.order.event;

import java.math.BigDecimal;

/**
 * Integration Event: OrderCreatedEvent (PUBLIC)
 * Dùng để giao tiếp giữa các module (Notification, Payment, AI, vv).
 * Thiết kế dưới dạng Record để đảm bảo tính bất biến và không leak Entity.
 */
public record OrderCreatedEvent(
    String correlationId,
    Long orderId,
    Long userId,
    String customerName,
    String customerEmail,
    BigDecimal totalAmount,
    String itemsSummary
) {}
