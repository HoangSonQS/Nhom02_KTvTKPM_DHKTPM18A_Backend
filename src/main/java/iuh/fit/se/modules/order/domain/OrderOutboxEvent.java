package iuh.fit.se.modules.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ord_outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderOutboxEvent {

    @Id
    private String id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderOutboxStatus status = OrderOutboxStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static OrderOutboxEvent create(String eventType, String payload) {
        return OrderOutboxEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .payload(payload)
                .status(OrderOutboxStatus.PENDING)
                .build();
    }

    public void markPublished() {
        this.status = OrderOutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = OrderOutboxStatus.FAILED;
    }
}
