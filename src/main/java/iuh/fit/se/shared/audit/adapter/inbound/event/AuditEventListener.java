package iuh.fit.se.shared.audit.adapter.inbound.event;

import iuh.fit.se.shared.audit.application.port.out.AuditLogWritePort;
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

    private final AuditLogWritePort auditLogWritePort;

    @Async
    @EventListener
    public void handleUserActionAudited(UserActionAuditedEvent event) {
        log.debug("Processing audit event async: {}", event.action());
        try {
            auditLogWritePort.save(new AuditLogWritePort.UserActionAuditRecord(
                    event.userId(),
                    event.role(),
                    event.action(),
                    event.target(),
                    event.oldValue(),
                    event.newValue(),
                    event.timestamp()
            ));
            
        } catch (Exception e) {
            log.error("Failed to persist audit log async", e);
        }
    }
}
