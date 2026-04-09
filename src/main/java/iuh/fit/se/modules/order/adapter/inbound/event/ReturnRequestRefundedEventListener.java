package iuh.fit.se.modules.order.adapter.inbound.event;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.returns.domain.ReturnRequestRefundedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnRequestRefundedEventListener {

    private final OrderInternalUseCase orderUseCase;

    @EventListener
    @Transactional
    public void onReturnRequestRefunded(ReturnRequestRefundedEvent event) {
        log.info("Received ReturnRequestRefundedEvent for orderId: {}", event.getOrderId());
        
        // Use the returned items from event payload to decide status
        // Here we trigger the internal use case to figure out RETURNED vs PARTIAL_RETURNED
        try {
            orderUseCase.processReturnCompleted(event.getOrderId());
            log.info("Successfully updated order status after return completed for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to update order status for order: {}", event.getOrderId(), e);
        }
    }
}
