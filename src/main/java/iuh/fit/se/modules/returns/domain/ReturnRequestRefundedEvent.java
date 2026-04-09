package iuh.fit.se.modules.returns.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReturnRequestRefundedEvent {
    private final UUID eventId;
    private final String returnRequestId;
    private final Long orderId;
    private final BigDecimal refundAmount;
    private final LocalDateTime occurredAt;

    public static ReturnRequestRefundedEvent of(String returnRequestId, Long orderId, BigDecimal refundAmount) {
        return ReturnRequestRefundedEvent.builder()
                .eventId(UUID.randomUUID())
                .returnRequestId(returnRequestId)
                .orderId(orderId)
                .refundAmount(refundAmount)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
