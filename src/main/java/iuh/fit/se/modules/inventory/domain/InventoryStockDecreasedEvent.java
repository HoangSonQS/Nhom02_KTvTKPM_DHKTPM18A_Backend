package iuh.fit.se.modules.inventory.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryStockDecreasedEvent {
    private final String eventId;
    private final Long bookId;
    private final int amount;
    private final int remainingQuantity;
    private final LocalDateTime occurredAt;

    public static InventoryStockDecreasedEvent create(Long bookId, int amount, int remaining) {
        return InventoryStockDecreasedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .bookId(bookId)
                .amount(amount)
                .remainingQuantity(remaining)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
