package iuh.fit.se.modules.order.adapter.inbound.event;

import iuh.fit.se.shared.event.returns.ReturnIntegrationEvents.ReturnRequestRefundedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReturnRequestRefundedEventListener {

    @EventListener
    public void onReturnRequestRefunded(ReturnRequestRefundedIntegrationEvent event) {
        // Audit-only listener. Order.fulfillmentStatus stays DELIVERED after a return —
        // ReturnRequest.returnStatus is the source of truth for return lifecycle (since V27).
        log.info("[Order Audit] ReturnRequestRefunded event received for orderId={}. " +
                "No order status change required — FulfillmentStatus remains DELIVERED.", event.orderId());
    }
}
