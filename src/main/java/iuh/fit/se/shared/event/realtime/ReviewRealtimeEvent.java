package iuh.fit.se.shared.event.realtime;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewRealtimeEvent(
        String id,
        String type,
        Long reviewId,
        Long bookId,
        Long userId,
        Integer rating,
        String handlingStatus,
        String message,
        LocalDateTime occurredAt
) implements Serializable {
    public static ReviewRealtimeEvent changed(Long reviewId, Long bookId, Long userId, int rating, String handlingStatus) {
        String type = rating <= 2 ? "REVIEW_NEEDS_ACTION" : "REVIEW_UPDATED";
        String message = rating <= 2
                ? "Có đánh giá " + rating + " sao cần xử lý"
                : "Có đánh giá mới";
        return new ReviewRealtimeEvent(
                UUID.randomUUID().toString(),
                type,
                reviewId,
                bookId,
                userId,
                rating,
                handlingStatus,
                message,
                LocalDateTime.now()
        );
    }
}
