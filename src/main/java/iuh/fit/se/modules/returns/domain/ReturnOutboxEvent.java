package iuh.fit.se.modules.returns.domain;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnOutboxStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static ReturnOutboxEvent create(String eventType, String payload) {
        return ReturnOutboxEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .payload(payload)
                .status(ReturnOutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
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
