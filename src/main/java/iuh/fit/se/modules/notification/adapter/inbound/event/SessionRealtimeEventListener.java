package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.realtime.SessionRealtimeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SessionRealtimeEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleSessionRealtime(SessionRealtimeEvent event) {
        RealtimeEventResponse payload = new RealtimeEventResponse(
                event.type(),
                null,
                event.userId(),
                null,
                null,
                null,
                null,
                event.message(),
                event.occurredAt()
        );
        notificationService.publishRealtimeToUserExceptDevice(event.userId(), event.activeDeviceId(), payload);
    }
}
