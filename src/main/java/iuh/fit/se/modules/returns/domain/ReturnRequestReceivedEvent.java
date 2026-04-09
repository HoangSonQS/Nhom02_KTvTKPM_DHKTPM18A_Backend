package iuh.fit.se.modules.returns.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ReturnRequestReceivedEvent {
    private final UUID eventId;
    private final String returnRequestId;
    private final Long orderId;
    private final List<ReturnedItemCondition> items;
    private final LocalDateTime occurredAt;

    @Getter
    @Builder
    public static class ReturnedItemCondition {
        private final Long bookId;
        private final Integer quantity;
        private final ItemCondition condition;
    }

    public static ReturnRequestReceivedEvent of(String returnRequestId, Long orderId, List<ReturnedItemCondition> items) {
        return ReturnRequestReceivedEvent.builder()
                .eventId(UUID.randomUUID())
                .returnRequestId(returnRequestId)
                .orderId(orderId)
                .items(items)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
