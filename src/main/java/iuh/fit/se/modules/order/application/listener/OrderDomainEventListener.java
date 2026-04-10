package iuh.fit.se.modules.order.application.listener;

import iuh.fit.se.modules.order.application.event.OrderCreatedIntegrationEvent;
import iuh.fit.se.modules.order.application.port.out.OrderEventPort;
import iuh.fit.se.modules.order.domain.Order;
import iuh.fit.se.modules.order.domain.event.OrderCreatedDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDomainEventListener {

    private final OrderEventPort orderEventPort;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderCreated(OrderCreatedDomainEvent domainEvent) {
        log.info("Handling OrderCreatedDomainEvent for order: {}", domainEvent.getOrder().getId());
        
        Order order = domainEvent.getOrder();
        
        // Mapping Domain Entity -> Integration Record (DTO)
        String itemsSummary = order.getItems().stream()
                .limit(5)
                .map(item -> item.getBookTitle() + " (x" + item.getQuantity() + ")")
                .collect(Collectors.joining(", "));
        
        if (order.getItems().size() > 5) {
            itemsSummary += "... và " + (order.getItems().size() - 5) + " sản phẩm khác";
        }

        OrderCreatedIntegrationEvent integrationEvent = new OrderCreatedIntegrationEvent(
                java.util.UUID.randomUUID().toString(),
                order.getRequestId(),
                order.getId(),
                order.getUserId(),
                domainEvent.getCustomerName(),
                domainEvent.getCustomerEmail(),
                order.getTotalAmount().subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : java.math.BigDecimal.ZERO),
                itemsSummary
        );

        // Publish to Outbox via Port
        orderEventPort.publishOrderCreated(integrationEvent);
    }
}
