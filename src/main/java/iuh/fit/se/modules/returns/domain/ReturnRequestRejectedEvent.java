package iuh.fit.se.modules.returns.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReturnRequestRejectedEvent {
    private final UUID eventId;
    private final String returnRequestId;
    private final Long orderId;
    private final String reason;
    private final LocalDateTime occurredAt;

    public static ReturnRequestRejectedEvent of(String returnRequestId, Long orderId, String reason) {
        return ReturnRequestRejectedEvent.builder()
                .eventId(UUID.randomUUID())
                .returnRequestId(returnRequestId)
                .orderId(orderId)
                .reason(reason)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
