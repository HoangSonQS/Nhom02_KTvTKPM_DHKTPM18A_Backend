package iuh.fit.se.modules.order.application.listener;

import iuh.fit.se.shared.event.order.OrderCreatedIntegrationEvent;
import iuh.fit.se.modules.order.application.port.out.OrderEventPort;
import iuh.fit.se.modules.order.domain.event.OrderCreatedDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDomainEventListener {

    private final OrderEventPort orderEventPort;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderCreated(OrderCreatedDomainEvent domainEvent) {
        log.info("Handling OrderCreatedDomainEvent for order: {}", domainEvent.getOrderId());

        OrderCreatedIntegrationEvent integrationEvent = OrderCreatedIntegrationEvent.of(
                domainEvent.getOrderId(),
                domainEvent.getUserId(),
                domainEvent.getCustomerName(),
                domainEvent.getCustomerEmail(),
                domainEvent.getTotalAmount(),
                domainEvent.getItemsSummary(),
                domainEvent.getCorrelationId());

        // Publish to Outbox via Port
        orderEventPort.publishOrderCreated(integrationEvent);
    }
}
