package iuh.fit.se.shared.event.realtime;

import java.time.LocalDateTime;

public record PurchaseOrderRealtimeEvent(
        String type,
        Long purchaseOrderId,
        String status,
        String message,
        LocalDateTime occurredAt
) {
    public static PurchaseOrderRealtimeEvent updated(Long purchaseOrderId, String status, String message) {
        return new PurchaseOrderRealtimeEvent(
                "PURCHASE_ORDER_UPDATED",
                purchaseOrderId,
                status,
                message,
                LocalDateTime.now()
        );
    }
}
