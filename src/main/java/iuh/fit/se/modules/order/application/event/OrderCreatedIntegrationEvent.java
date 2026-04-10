package iuh.fit.se.modules.order.application.event;

import java.math.BigDecimal;

/**
 * Integration Event: OrderCreatedIntegrationEvent (PUBLIC)
 * Dùng để giao tiếp giữa các module. 
 * Chỉ chứa các kiểu dữ liệu cơ bản, không chứa Entity.
 */
public record OrderCreatedIntegrationEvent(
    String id,
    String correlationId,
    Long orderId,
    Long userId,
    String customerName,
    String customerEmail,
    BigDecimal totalAmount,
    String itemsSummary
) {}
