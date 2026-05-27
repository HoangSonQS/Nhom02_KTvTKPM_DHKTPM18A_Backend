package iuh.fit.se.modules.order.application.listener;

import iuh.fit.se.shared.event.returns.ReturnIntegrationEvents.ReturnRequestApprovedIntegrationEvent;
import iuh.fit.se.shared.event.returns.ReturnIntegrationEvents.ReturnRequestRefundedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener nhận return events từ module Returns.
 *
 * <p>Kể từ V27 migration: Order.fulfillmentStatus KHÔNG thay đổi khi có return request.
 * Return lifecycle được track hoàn toàn bởi ReturnRequest.returnStatus trong module Returns.
 *
 * <p>Listener này hiện chỉ ghi log audit — không còn gọi processReturnCompleted().
 *
 * <p>TODO (migration): Quyết định sau V28 xem có cần keep listener này hay remove hoàn toàn.
 * Nếu cần emit OrderReturnApprovedEvent cho downstream systems, add logic tại đây.
 */
@Component
@Slf4j
public class ReturnIntegrationEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReturnApproved(ReturnRequestApprovedIntegrationEvent event) {
        // V27 migration: Order.fulfillmentStatus giữ nguyên DELIVERED.
        // Return state được track bởi ReturnRequest.returnStatus.
        log.info("[V27] ReturnRequestApprovedIntegrationEvent received for Order {}. " +
                "Order fulfillmentStatus remains DELIVERED. Return tracked via ReturnRequest.", event.orderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReturnRefunded(ReturnRequestRefundedIntegrationEvent event) {
        // V27 migration: No-op — chỉ log để audit trail.
        log.info("[V27] ReturnRequestRefundedIntegrationEvent received for Order {}. " +
                "Order fulfillmentStatus unchanged. Refund tracked via ReturnRequest.", event.orderId());
    }
}
