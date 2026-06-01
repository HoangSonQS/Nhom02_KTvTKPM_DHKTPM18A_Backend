package iuh.fit.se.modules.notification.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RealtimeEventResponse(
        String type,
        Long orderId,
        Long userId,
        Long bookId,
        Long reviewId,
        String returnRequestId,
        Long purchaseOrderId,
        Long stocktakeId,
        Integer quantity,
        BigDecimal amount,
        String status,
        String message,
        LocalDateTime occurredAt
) {
    public RealtimeEventResponse(
            String type,
            Long orderId,
            Long userId,
            Long bookId,
            Integer quantity,
            BigDecimal amount,
            String status,
            String message,
            LocalDateTime occurredAt
    ) {
        this(type, orderId, userId, bookId, null, null, null, null, quantity, amount, status, message, occurredAt);
    }
}
