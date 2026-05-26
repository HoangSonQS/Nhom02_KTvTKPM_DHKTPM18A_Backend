package iuh.fit.se.modules.notification.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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
    private String eventId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "channel", length = 10)
    private String channel;

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

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public void markSuccess() {
        this.status = NotificationStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementAttempt(String error) {
        this.attemptCount++;
        this.lastError = error;
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
    }

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }
}
