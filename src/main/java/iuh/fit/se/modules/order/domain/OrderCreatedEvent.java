package iuh.fit.se.modules.order.domain;

import iuh.fit.se.shared.domain.BaseEvent;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * OrderCreatedEvent — Sự kiện đơn hàng được khởi tạo thành công (Saga Completed).
 * Chứa đầy đủ thông tin để gửi mail và làm báo cáo (Self-contained).
 */
@Getter
@SuperBuilder
public class OrderCreatedEvent extends BaseEvent {
    private final Long orderId;
    private final Long userId;
    private final String customerName;
    private final String customerEmail;
    private final BigDecimal totalAmount;
    private final String itemsSummary; // JSON hoặc String rút gọn (Top 3-5 items)

    public static OrderCreatedEvent create(Order order, String customerName, String customerEmail) {
        String itemsText = order.getItems().stream()
                .limit(5)
                .map(item -> item.getBookTitle() + " (x" + item.getQuantity() + ")")
                .collect(Collectors.joining(", "));
        
        if (order.getItems().size() > 5) {
            itemsText += "... và " + (order.getItems().size() - 5) + " sản phẩm khác";
        }

        return OrderCreatedEvent.builder()
                .correlationId(order.getRequestId())
                .eventType("ORDER_CREATED")
                .orderId(order.getId())
                .userId(order.getUserId())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .totalAmount(order.getTotalAmount().subtract(order.getDiscountAmount()))
                .itemsSummary(itemsText)
                .build();
    }
}
