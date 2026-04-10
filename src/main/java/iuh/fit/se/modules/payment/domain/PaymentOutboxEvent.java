package iuh.fit.se.modules.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pay_outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOutboxEvent {

    @Id
    private String id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentOutboxStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static PaymentOutboxEvent create(String eventType, String payload) {
        return PaymentOutboxEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .payload(payload)
                .status(PaymentOutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markPublished() {
        this.status = PaymentOutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentOutboxStatus.FAILED;
    }
}
