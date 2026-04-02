package iuh.fit.se.modules.notification.application.port.in;

import iuh.fit.se.modules.notification.domain.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * NotificationLogResponse — DTO trả về thông tin log cho các module quản trị (Admin).
 * Đảm bảo tính đóng gói (Encapsulation), không để lộ JPA Entity ra ngoài module.
 */
@Data
@Builder
public class NotificationLogResponse {
    private Long id;
    private String eventId;
    private Long orderId;
    private NotificationStatus status;
    private String channel;
    private int attemptCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
