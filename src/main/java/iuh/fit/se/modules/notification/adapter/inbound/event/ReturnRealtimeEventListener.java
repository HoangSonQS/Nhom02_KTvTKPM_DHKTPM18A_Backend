package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.realtime.ReturnRealtimeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReturnRealtimeEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleReturnRealtime(ReturnRealtimeEvent event) {
        RealtimeEventResponse payload = new RealtimeEventResponse(
                event.type(),
                event.orderId(),
                event.userId(),
                null,
                null,
                null,
                event.status(),
                event.message(),
                event.occurredAt()
        );
        notificationService.publishRealtimeToUser(event.userId(), payload);
    }
}
