package iuh.fit.se.modules.order.adapter.inbound.event;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.order.application.port.out.PromotionPort;
import iuh.fit.se.shared.event.payment.PaymentSuccessIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentEventListener {

    private final OrderInternalUseCase orderUseCase;
    private final PromotionPort promotionPort;

    @EventListener
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessIntegrationEvent event) {
        log.info("PaymentSuccessIntegrationEvent received for Order {}. Finalizing status and coupon...", event.orderId());
        
        // 1. Mark Order as PAID (Idempotent)
        orderUseCase.markOrderAsPaid(event.orderId());
        
        // 2. Confirm Coupon Usage — requestId is carried from checkout saga via event chain.
        //    confirmCouponUsage is idempotent: only acts if reservation is still RESERVED.
        if (event.orderRequestId() != null) {
            promotionPort.confirmCouponUsage(event.orderRequestId());
            log.info("Coupon usage confirmed for order request: {}", event.orderRequestId());
        }
        
        log.info("Successfully finalized Order {} processing.", event.orderId());
    }
}
