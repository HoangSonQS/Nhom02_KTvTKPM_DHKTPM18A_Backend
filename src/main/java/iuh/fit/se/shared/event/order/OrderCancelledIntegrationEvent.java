package iuh.fit.se.shared.event.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * OrderCancelledIntegrationEvent — Sự kiện tích hợp khi đơn hàng bị hủy.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCancelledIntegrationEvent implements Serializable {
    @Builder.Default
    private final UUID eventId = UUID.randomUUID();
    
    private Long orderId;
    private String reason;
}
