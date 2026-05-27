package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.realtime.OrderRealtimeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class OrderRealtimeEventListener {

    private static final Set<String> ORDER_MANAGEMENT_ROLES = Set.of("ADMIN", "STAFF_SELLER");

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOrderRealtime(OrderRealtimeEvent event) {
        RealtimeEventResponse payload = new RealtimeEventResponse(
                event.type(),
                event.orderId(),
                event.userId(),
                null,
                null,
                event.amount(),
                event.status(),
                event.message(),
                event.occurredAt()
        );
        notificationService.publishRealtimeToRoles(ORDER_MANAGEMENT_ROLES, payload);
        notificationService.publishRealtimeToUser(event.userId(), payload);
    }
}
