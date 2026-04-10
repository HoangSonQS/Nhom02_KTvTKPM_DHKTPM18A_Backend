package iuh.fit.se.modules.order.domain;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderOutboxStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static OrderOutboxEvent create(String eventType, String payload) {
        return OrderOutboxEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .payload(payload)
                .status(OrderOutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
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
