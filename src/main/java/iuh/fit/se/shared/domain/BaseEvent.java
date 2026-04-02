package iuh.fit.se.shared.domain;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * BaseEvent — Mẫu chuẩn cho mọi sự kiện trong hệ thống (Staff+ Standard).
 * Giúp tracing, auditing và xử lý idempotency.
 */
@Getter
@SuperBuilder
public abstract class BaseEvent {
    private final String eventId;      // UUID duy nhất cho mỗi event
    private final String correlationId; // Ánh xạ từ requestId của saga
    private final String eventType;     // Loại event (ORDER_CREATED, etc.)
    private final Instant occurredAt;   // Thời điểm xảy ra
    private final int eventVersion;     // Phiên bản schema của event

    protected BaseEvent(String correlationId, String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.correlationId = correlationId;
        this.eventType = eventType;
        this.occurredAt = Instant.now();
        this.eventVersion = 1;
    }
}
