package iuh.fit.se.shared.audit.adapter.inbound.event;

import iuh.fit.se.shared.audit.application.port.out.AuditLogWritePort;
import iuh.fit.se.shared.audit.domain.event.UserActionAuditedEvent;
import iuh.fit.se.shared.event.realtime.DataChangedRealtimeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class AuditEventListenerTest {

    @Test
    void whenAuditRecordIsSaved_thenPublishAuditLogChanged() {
        AuditLogWritePort auditLogWritePort = mock(AuditLogWritePort.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        AuditEventListener listener = new AuditEventListener(auditLogWritePort, eventPublisher);
        UserActionAuditedEvent event = new UserActionAuditedEvent(
                "5", "ADMIN", "ADMIN_LOCK_USER", "user:7", null, "locked", Instant.now());

        listener.handleUserActionAudited(event);

        var ordered = inOrder(auditLogWritePort, eventPublisher);
        ordered.verify(auditLogWritePort).save(any(AuditLogWritePort.UserActionAuditRecord.class));
        ordered.verify(eventPublisher).publishEvent(any(DataChangedRealtimeEvent.class));
    }
}
