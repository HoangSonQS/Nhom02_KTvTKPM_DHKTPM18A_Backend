package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.realtime.ReviewRealtimeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReviewRealtimeEventListener {

    private static final Set<String> REVIEW_AFFECTED_ROLES = Set.of("ADMIN", "CUSTOMER", "PUBLIC");

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleReviewRealtime(ReviewRealtimeEvent event) {
        notificationService.publishRealtimeToRoles(REVIEW_AFFECTED_ROLES, new RealtimeEventResponse(
                event.type(),
                null,
                event.userId(),
                event.bookId(),
                event.reviewId(),
                null,
                null,
                null,
                event.rating(),
                null,
                event.handlingStatus(),
                event.message(),
                event.occurredAt()
        ));
    }
}
