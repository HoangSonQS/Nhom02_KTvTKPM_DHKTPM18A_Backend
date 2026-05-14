package iuh.fit.se.shared.audit.application.port.out;

import iuh.fit.se.shared.audit.application.port.in.AuditLogQueryUseCase;

import java.util.List;

public interface AuditLogQueryPort {
    List<AuditLogQueryUseCase.AuditLogResponse> findRecentStaffLogs(List<String> roles);
}
