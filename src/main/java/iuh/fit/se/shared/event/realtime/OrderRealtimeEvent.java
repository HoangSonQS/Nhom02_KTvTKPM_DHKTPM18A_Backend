package iuh.fit.se.shared.event.realtime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderRealtimeEvent(
        String id,
        String type,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String status,
        String message,
        LocalDateTime occurredAt
) implements Serializable {
    public static OrderRealtimeEvent orderCreated(Long orderId, Long userId, BigDecimal amount, String status) {
        return new OrderRealtimeEvent(
                UUID.randomUUID().toString(),
                "ORDER_CREATED",
                orderId,
                userId,
                amount,
                status,
                "Có đơn hàng mới #" + orderId,
                LocalDateTime.now()
        );
    }

    public static OrderRealtimeEvent statusChanged(Long orderId, Long userId, BigDecimal amount, String status) {
        return new OrderRealtimeEvent(
                UUID.randomUUID().toString(),
                "ORDER_STATUS_CHANGED",
                orderId,
                userId,
                amount,
                status,
                "Đơn hàng #" + orderId + " đã được cập nhật trạng thái",
                LocalDateTime.now()
        );
    }
}
