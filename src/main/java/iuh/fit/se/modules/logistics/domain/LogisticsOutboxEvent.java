package iuh.fit.se.modules.logistics.domain;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "log_outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogisticsOutboxEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogisticsOutboxStatus status = LogisticsOutboxStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static LogisticsOutboxEvent create(String eventType, String payload) {
        return LogisticsOutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .payload(payload)
                .status(LogisticsOutboxStatus.PENDING)
                .build();
    }

    public void markPublished() {
        this.status = LogisticsOutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = LogisticsOutboxStatus.FAILED;
    }
}
