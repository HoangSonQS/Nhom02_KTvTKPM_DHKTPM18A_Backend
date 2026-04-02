package iuh.fit.se.modules.order.domain;

import iuh.fit.se.shared.domain.BaseEvent;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * OrderCancelledEvent — Sự kiện đơn hàng bị hủy (do hết hạn hoặc lỗi Saga).
 */
@Getter
@SuperBuilder
public class OrderCancelledEvent extends BaseEvent {
    private final Long orderId;
    private final String reason;

    public static OrderCancelledEvent create(Order order, String reason) {
        return OrderCancelledEvent.builder()
                .correlationId(order.getRequestId())
                .eventType("ORDER_CANCELLED")
                .orderId(order.getId())
                .reason(reason)
                .build();
    }
}
