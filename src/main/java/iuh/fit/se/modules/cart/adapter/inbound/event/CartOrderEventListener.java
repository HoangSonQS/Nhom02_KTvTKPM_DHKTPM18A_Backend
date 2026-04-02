package iuh.fit.se.modules.cart.adapter.inbound.event;

import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.modules.order.domain.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartOrderEventListener {

    private final CartInternalUseCase cartUseCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("OrderCreatedEvent received for order {}. Clearing cart for user {}", 
                event.getOrderId(), event.getUserId());
        try {
            cartUseCase.clearCart(event.getUserId());
            log.info("Cart cleared successfully for user {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to clear cart for user {} after order {}: {}", 
                    event.getUserId(), event.getOrderId(), e.getMessage());
            // We swallow exception here as cart cleanup is non-critical to order flow
        }
    }
}
