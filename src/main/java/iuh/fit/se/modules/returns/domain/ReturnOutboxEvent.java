package iuh.fit.se.modules.returns.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ret_outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnOutboxEvent {

    @Id
    private String id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnOutboxStatus status = ReturnOutboxStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static ReturnOutboxEvent create(String eventType, String payload) {
        return ReturnOutboxEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .payload(payload)
                .status(ReturnOutboxStatus.PENDING)
                .build();
    }

    public void markPublished() {
        this.status = ReturnOutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = ReturnOutboxStatus.FAILED;
    }
}
