package iuh.fit.se.modules.notification.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * NotificationLog — Lưu vết việc xử lý các sự kiện thông báo (Staff+ Standard).
 * Chống xử lý trùng lặp (Idempotency) và quản lý Retry/Dead-letter.
 */
@Entity
@Table(name = "ntf_notification_log", indexes = {
        @Index(name = "idx_ntf_event_id", columnList = "event_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 50)
    private String eventId; // Unique event ID từ metadata

    @Column(name = "order_id")
    private Long orderId;   // ID đơn hàng liên quan

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "channel", length = 10)
    private String channel; // EMAIL, SMS, PUSH

    @Column(name = "attempt_count")
    private int attemptCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public void markSuccess() {
        this.status = NotificationStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementAttempt(String error) {
        this.attemptCount++;
        this.lastError = error;
        // Trạng thái này chỉ biểu thị thất bại tạm thời (đang retry)
        this.status = NotificationStatus.FAILED;
    }

    public void markPermanentFailure(String error) {
        this.status = NotificationStatus.FAILED_PERMANENT;
        this.lastError = error;
        this.processedAt = LocalDateTime.now();
    }

    public void resetToInit() {
        this.status = NotificationStatus.INIT;
        this.processedAt = null;
        // Giữ lại lastError cũ và attemptCount cũ để audit. 
        // Khi NotificationSender chạy lại, nó sẽ tự increment tiếp.
    }
}
