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
    private final Long orderId;
    private final Long userId;
    private final String customerName;
    private final String customerEmail;
    private final java.math.BigDecimal totalAmount;
    private final String itemsSummary;

    public static OrderCreatedDomainEvent of(Order order, String customerName, String customerEmail) {
        String itemsText = order.getItems().stream()
                .limit(5)
                .map(item -> item.getBookTitle() + " (x" + item.getQuantity() + ")")
                .collect(java.util.stream.Collectors.joining(", "));

        if (order.getItems().size() > 5) {
            itemsText += "... và " + (order.getItems().size() - 5) + " sản phẩm khác";
        }

        return OrderCreatedDomainEvent.builder()
                .correlationId(order.getRequestId())
                .eventType("ORDER_CREATED_DOMAIN")
                .orderId(order.getId())
                .userId(order.getUserId())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .totalAmount(order.getTotalAmount().subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : java.math.BigDecimal.ZERO))
                .itemsSummary(itemsText)
                .build();
    }
}
