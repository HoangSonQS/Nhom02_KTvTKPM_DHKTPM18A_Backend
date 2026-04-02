package iuh.fit.se.modules.order.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderCreatedEvent {
    private final Long orderId;
    private final Long userId;
    private final String requestId;

    public static OrderCreatedEvent create(Order order) {
        return OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .requestId(order.getRequestId())
                .build();
    }
}
