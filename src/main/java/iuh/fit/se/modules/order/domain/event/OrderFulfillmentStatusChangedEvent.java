package iuh.fit.se.modules.order.domain.event;

import iuh.fit.se.modules.order.domain.FulfillmentStatus;
import iuh.fit.se.modules.order.domain.Order;
import iuh.fit.se.shared.domain.BaseEvent;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class OrderFulfillmentStatusChangedEvent extends BaseEvent {
    private final Long orderId;
    private final Long userId;
    private final String customerName;
    private final String customerEmail;
    private final FulfillmentStatus fromStatus;
    private final FulfillmentStatus toStatus;
    private final String reason;

    public static OrderFulfillmentStatusChangedEvent of(
            Order order,
            FulfillmentStatus fromStatus,
            FulfillmentStatus toStatus,
            String reason,
            String customerName,
            String customerEmail
    ) {
        return OrderFulfillmentStatusChangedEvent.builder()
                .correlationId(order.getRequestId())
                .eventType("ORDER_FULFILLMENT_STATUS_CHANGED")
                .orderId(order.getId())
                .userId(order.getUserId())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .build();
    }
}
