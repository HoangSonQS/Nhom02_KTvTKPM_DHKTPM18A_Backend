package iuh.fit.se.shared.audit.adapter.outbound.persistence;

import iuh.fit.se.shared.audit.application.port.in.AuditLogQueryUseCase;
import iuh.fit.se.shared.audit.application.port.out.AuditLogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditLogQueryPersistenceAdapter implements AuditLogQueryPort {

    private final AuditLogJpaRepository auditLogJpaRepository;

    @Override
    public List<AuditLogQueryUseCase.AuditLogResponse> findRecentStaffLogs(List<String> roles) {
        return auditLogJpaRepository.findTop100ByRoleInOrderByCreatedAtDesc(roles).stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogQueryUseCase.AuditLogResponse toResponse(AuditLogJpaEntity entity) {
        return new AuditLogQueryUseCase.AuditLogResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getRole(),
                entity.getAction(),
                entity.getTarget(),
                entity.getOldValue(),
                entity.getNewValue(),
                entity.getCreatedAt());
    }
}
