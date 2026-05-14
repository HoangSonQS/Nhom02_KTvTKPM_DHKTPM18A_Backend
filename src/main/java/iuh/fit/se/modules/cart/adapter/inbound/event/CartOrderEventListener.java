package iuh.fit.se.modules.cart.adapter.inbound.event;

import iuh.fit.se.modules.order.domain.event.OrderCreatedDomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class CartOrderEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedDomainEvent event) {
        log.debug("OrderCreatedEvent received for order {}. Cart cleanup is handled by checkout service.",
                event.getOrderId());
    }
}
