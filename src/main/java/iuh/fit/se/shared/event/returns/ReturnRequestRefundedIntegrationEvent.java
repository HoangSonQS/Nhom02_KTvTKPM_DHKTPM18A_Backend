package iuh.fit.se.shared.event.returns;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ReturnRequestRefundedIntegrationEvent — Sự kiện tích hợp khi hoàn tiền đổi trả.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReturnRequestRefundedIntegrationEvent implements Serializable {
    @Builder.Default
    private final UUID eventId = UUID.randomUUID();
    
    private Long orderId;
    private BigDecimal refundAmount;
    private LocalDateTime occurredAt;
}
