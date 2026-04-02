package iuh.fit.se.modules.order.adapter.inbound.event;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.order.application.port.out.PromotionPort;
import iuh.fit.se.modules.payment.domain.PaymentSuccessEvent;
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
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("PaymentSuccessEvent received for Order {}. Finalizing status and coupon...", event.getOrderId());
        
        // 1. Mark Order as PAID (Idempotent update)
        orderUseCase.markOrderAsPaid(event.getOrderId());
        
        // 2. Confirm Coupon Usage synchronously in the same transaction
        OrderInternalUseCase.OrderResponse order = orderUseCase.getOrderById(event.getOrderId());
        
        // Only confirm if it was reserved (Optional logic but safe if promotionPort handles it)
        promotionPort.confirmCouponUsage(order.getRequestId());
        
        log.info("Successfully finalized Order {} processing.", event.getOrderId());
    }
}
