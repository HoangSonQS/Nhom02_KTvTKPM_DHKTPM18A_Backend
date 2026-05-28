package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.audit.domain.event.UserActionAuditedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuditRealtimeEventListener {

    private static final Set<String> MANAGEMENT_ROLES = Set.of("ADMIN", "STAFF_SELLER", "STAFF_WAREHOUSE");
    private static final Set<String> NON_MUTATING_ACTIONS = Set.of("USER_LOGIN");

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserActionAudited(UserActionAuditedEvent event) {
        if (NON_MUTATING_ACTIONS.contains(event.action())) {
            return;
        }

        notificationService.publishRealtimeToRoles(MANAGEMENT_ROLES, new RealtimeEventResponse(
                "ADMIN_DATA_CHANGED",
                null,
                null,
                null,
                null,
                null,
                event.action(),
                buildMessage(event),
                LocalDateTime.ofInstant(event.timestamp(), ZoneId.systemDefault())
        ));
    }

    private String buildMessage(UserActionAuditedEvent event) {
        String target = event.target() == null || event.target().isBlank() ? "" : ": " + event.target();
        return "Dữ liệu hệ thống đã thay đổi" + target;
    }
}
