package iuh.fit.se.shared.event.realtime;

import java.time.LocalDateTime;

public record DataChangedRealtimeEvent(
        String type,
        String source,
        Long userId,
        Long bookId,
        Long reviewId,
        String returnRequestId,
        Long purchaseOrderId,
        Long stocktakeId,
        String status,
        String message,
        LocalDateTime occurredAt
) {
    public static DataChangedRealtimeEvent of(String type, String source, String message) {
        return new DataChangedRealtimeEvent(type, source, null, null, null, null, null, null, null, message, LocalDateTime.now());
    }

    public static DataChangedRealtimeEvent forBook(String type, Long bookId, String message) {
        return new DataChangedRealtimeEvent(type, "BOOK", null, bookId, null, null, null, null, null, message, LocalDateTime.now());
    }

    public static DataChangedRealtimeEvent forUser(String type, Long userId, String message) {
        return new DataChangedRealtimeEvent(type, "USER", userId, null, null, null, null, null, null, message, LocalDateTime.now());
    }

    public static DataChangedRealtimeEvent stocktake(Long stocktakeId, String status, String message) {
        return new DataChangedRealtimeEvent(
                "STOCKTAKE_UPDATED",
                "STOCKTAKE",
                null,
                null,
                null,
                null,
                null,
                stocktakeId,
                status,
                message,
                LocalDateTime.now()
        );
    }
}
