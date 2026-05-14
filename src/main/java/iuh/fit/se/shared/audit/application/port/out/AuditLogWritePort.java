package iuh.fit.se.shared.audit.application.port.out;

import java.time.Instant;

public interface AuditLogWritePort {
    void save(UserActionAuditRecord record);

    record UserActionAuditRecord(
            String userId,
            String role,
            String action,
            String target,
            String oldValue,
            String newValue,
            Instant createdAt
    ) {}
}
