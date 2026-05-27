package iuh.fit.se.shared.event.inventory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryStockChangedIntegrationEvent(
        String id,
        Long bookId,
        int amount,
        int remainingQuantity,
        String changeType,
        LocalDateTime occurredAt
) implements Serializable {
    public static InventoryStockChangedIntegrationEvent of(Long bookId, int amount, int remainingQuantity, String changeType) {
        return new InventoryStockChangedIntegrationEvent(
                UUID.randomUUID().toString(),
                bookId,
                amount,
                remainingQuantity,
                changeType,
                LocalDateTime.now()
        );
    }
}
