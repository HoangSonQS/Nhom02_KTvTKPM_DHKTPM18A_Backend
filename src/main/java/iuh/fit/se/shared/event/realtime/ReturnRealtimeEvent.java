package iuh.fit.se.shared.event.realtime;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReturnRealtimeEvent(
        String id,
        String type,
        String returnRequestId,
        Long orderId,
        Long userId,
        String status,
        String message,
        LocalDateTime occurredAt
) implements Serializable {
    public static ReturnRealtimeEvent statusChanged(String returnRequestId, Long orderId, Long userId, String status) {
        return new ReturnRealtimeEvent(
                UUID.randomUUID().toString(),
                "RETURN_STATUS_CHANGED",
                returnRequestId,
                orderId,
                userId,
                status,
                "Yeu cau tra hang #" + returnRequestId + " da duoc cap nhat trang thai",
                LocalDateTime.now()
        );
    }
}
