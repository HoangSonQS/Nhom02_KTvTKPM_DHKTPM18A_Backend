package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.realtime.PurchaseOrderRealtimeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class PurchaseOrderRealtimeEventListener {

    private static final Set<String> PURCHASE_ORDER_MANAGEMENT_ROLES = Set.of("ADMIN", "STAFF_WAREHOUSE");

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handlePurchaseOrderRealtime(PurchaseOrderRealtimeEvent event) {
        notificationService.publishRealtimeToRoles(PURCHASE_ORDER_MANAGEMENT_ROLES, new RealtimeEventResponse(
                event.type(),
                null,
                null,
                null,
                null,
                null,
                event.purchaseOrderId(),
                null,
                null,
                null,
                event.status(),
                event.message(),
                event.occurredAt()
        ));
    }
}
