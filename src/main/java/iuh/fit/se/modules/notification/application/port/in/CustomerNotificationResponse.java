package iuh.fit.se.modules.notification.application.port.in;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CustomerNotificationResponse(
        Long id,
        Long orderId,
        String title,
        String message,
        String channel,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
