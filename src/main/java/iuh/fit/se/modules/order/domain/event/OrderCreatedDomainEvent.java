package iuh.fit.se.modules.order.domain.event;

import iuh.fit.se.modules.order.domain.Order;
import iuh.fit.se.shared.domain.BaseEvent;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Domain Event: OrderCreatedDomainEvent (Module-Internal)
 * Là sự kiện nội bộ phát ra từ Domain Layer khi đơn hàng hoàn tất checkout.
 */
@Getter
@SuperBuilder
public class OrderCreatedDomainEvent extends BaseEvent {
    private final Order order;
    private final String customerName;
    private final String customerEmail;

    public static OrderCreatedDomainEvent of(Order order, String customerName, String customerEmail) {
        return OrderCreatedDomainEvent.builder()
                .correlationId(order.getRequestId())
                .eventType("ORDER_CREATED_DOMAIN")
                .order(order)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .build();
    }
}
