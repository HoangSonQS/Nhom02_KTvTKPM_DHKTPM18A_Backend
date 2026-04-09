package iuh.fit.se.modules.returns.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReturnRequestApprovedEvent {
    private final UUID eventId;
    private final String returnRequestId;
    private final Long orderId;
    private final LocalDateTime occurredAt;

    public static ReturnRequestApprovedEvent of(String returnRequestId, Long orderId) {
        return ReturnRequestApprovedEvent.builder()
                .eventId(UUID.randomUUID())
                .returnRequestId(returnRequestId)
                .orderId(orderId)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
