package iuh.fit.se.shared.audit.application.port.in;

import java.time.Instant;
import java.util.List;

public interface AuditLogQueryUseCase {

    List<AuditLogResponse> listRecentLogs();

    record AuditLogResponse(
            Long id,
            String userId,
            String role,
            String action,
            String target,
            String oldValue,
            String newValue,
            Instant createdAt
    ) {}
}
