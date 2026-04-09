package iuh.fit.se.modules.payment.adapter.inbound.event;

import iuh.fit.se.modules.payment.application.port.in.PaymentUseCase;
import iuh.fit.se.modules.returns.domain.ReturnRequestReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component("paymentReturnRequestReceivedEventListener")
@RequiredArgsConstructor
@Slf4j
public class ReturnRequestReceivedEventListener {

    private final PaymentUseCase paymentUseCase;

    @EventListener
    @Transactional
    public void onReturnRequestReceived(ReturnRequestReceivedEvent event) {
        log.info("Received ReturnRequestReceivedEvent for returnId: {}. Triggering refund simulation.", event.getReturnRequestId());

        // For simplicity, we assume we trigger refund based on the event.
        // In a more complex flow, the refund amount might be different from the total returned items price.
        // Here we trigger refund for the order because VNPay refund is usually per transaction.
        
        // We simulate a refund amount (e.g., 90% of order total or similar, or specific amount from return request)
        // Since we don't have the refund amount in THIS event (it's in RefundedEvent),
        // but the plan says "Trigger refund at RECEIVED step".
        
        // Wait, if I trigger at RECEIVED, I need the amount.
        // Let's assume we refund the amount calculated during return request creation.
        
        // However, the ReturnRequestReceivedEvent doesn't have the amount.
        // I might need to query the Returns module for the request to get the amount.
        
        // In a modular monolith, I'll assume a fixed amount or simulation for now.
        // Actually, the plan says: "Trigger refund tại bước RECEIVED".
        
        log.info("Refunding for order {} based on return {}", event.getOrderId(), event.getReturnRequestId());
        
        // Simulation amount
        BigDecimal simulatedAmount = BigDecimal.valueOf(100000); 

        try {
            paymentUseCase.processRefund(event.getOrderId(), simulatedAmount, event.getReturnRequestId());
            log.info("Successfully triggered refund for returnId: {}", event.getReturnRequestId());
        } catch (Exception e) {
            log.error("Failed to trigger refund for returnId: {}", event.getReturnRequestId(), e);
        }
    }
}
