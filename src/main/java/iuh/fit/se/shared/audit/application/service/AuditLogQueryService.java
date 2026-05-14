package iuh.fit.se.shared.audit.application.service;

import iuh.fit.se.shared.audit.application.port.in.AuditLogQueryUseCase;
import iuh.fit.se.shared.audit.application.port.out.AuditLogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService implements AuditLogQueryUseCase {

    private final AuditLogQueryPort auditLogQueryPort;

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> listRecentLogs() {
        return auditLogQueryPort.findRecentStaffLogs(List.of("STAFF_SELLER", "STAFF_WAREHOUSE"));
    }
}
