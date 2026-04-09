package iuh.fit.se.modules.returns.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReturnRequestCreatedEvent {
    private final java.util.UUID eventId;
    private final String returnRequestId;
    private final Long orderId;
    private final Long customerId;
    private final LocalDateTime occurredAt;

    public static ReturnRequestCreatedEvent of(String returnRequestId, Long orderId, Long customerId) {
        return ReturnRequestCreatedEvent.builder()
                .eventId(java.util.UUID.randomUUID())
                .returnRequestId(returnRequestId)
                .orderId(orderId)
                .customerId(customerId)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
