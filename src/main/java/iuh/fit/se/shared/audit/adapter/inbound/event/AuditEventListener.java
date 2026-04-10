package iuh.fit.se.shared.audit.adapter.inbound.event;

import iuh.fit.se.shared.audit.adapter.outbound.persistence.AuditLogJpaEntity;
import iuh.fit.se.shared.audit.adapter.outbound.persistence.AuditLogJpaRepository;
import iuh.fit.se.shared.audit.domain.event.UserActionAuditedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Inbound Adapter lắng nghe Domain Event và xử lý bất đồng bộ.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogJpaRepository auditLogJpaRepository;

    @Async
    @EventListener
    public void handleUserActionAudited(UserActionAuditedEvent event) {
        log.debug("Processing audit event async: {}", event.action());
        try {
            AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
                .userId(event.userId())
                .action(event.action())
                .target(event.target())
                .oldValue(event.oldValue())
                .newValue(event.newValue())
                .createdAt(event.timestamp())
                .build();
            
            auditLogJpaRepository.save(entity);
            
        } catch (Exception e) {
            log.error("Failed to persist audit log async", e);
        }
    }
}
