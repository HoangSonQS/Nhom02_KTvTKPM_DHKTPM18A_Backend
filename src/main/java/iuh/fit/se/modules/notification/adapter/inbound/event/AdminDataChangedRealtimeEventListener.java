package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.realtime.AdminDataChangedRealtimeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminDataChangedRealtimeEventListener {

    private static final Set<String> MANAGEMENT_ROLES = Set.of("ADMIN", "STAFF_SELLER", "STAFF_WAREHOUSE");

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleAdminDataChanged(AdminDataChangedRealtimeEvent event) {
        notificationService.publishRealtimeToRoles(MANAGEMENT_ROLES, new RealtimeEventResponse(
                "ADMIN_DATA_CHANGED",
                null,
                null,
                null,
                null,
                null,
                event.source(),
                event.message(),
                event.occurredAt()
        ));
    }
}
