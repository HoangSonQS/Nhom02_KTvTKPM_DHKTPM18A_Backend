package iuh.fit.se.shared.audit.domain.event;

import java.time.Instant;

/**
 * Domain Event cho việc kiểm toán người dùng.
 * Tuân thủ Hexagonal: POJO thuần, không phụ thuộc Framework.
 */
public record UserActionAuditedEvent(
    String userId,
    String action,
    String target,
    String oldValue,
    String newValue,
    Instant timestamp
) {}
