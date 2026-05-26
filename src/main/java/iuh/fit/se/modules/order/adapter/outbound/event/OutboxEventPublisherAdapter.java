package iuh.fit.se.modules.order.adapter.outbound.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.order.adapter.outbound.persistence.JpaOrderOutboxRepository;
import iuh.fit.se.shared.event.order.OrderCreatedIntegrationEvent;
import iuh.fit.se.shared.event.order.OrderStatusChangedIntegrationEvent;
import iuh.fit.se.modules.order.application.port.out.OrderEventPort;
import iuh.fit.se.modules.order.domain.OrderOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("orderOutboxEventPublisherAdapter")
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherAdapter implements OrderEventPort {

    private final JpaOrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publishOrderCreated(OrderCreatedIntegrationEvent event) {
        publish("OrderCreatedIntegrationEvent", event);
    }

    @Override
    public void publishOrderStatusChanged(OrderStatusChangedIntegrationEvent event) {
        publish("OrderStatusChangedIntegrationEvent", event);
    }

    private void publish(String eventType, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OrderOutboxEvent outboxEvent = OrderOutboxEvent.create(eventType, payload);
            outboxRepository.save(outboxEvent);
            log.info("Saved {} to outbox: {}", eventType, outboxEvent.getId());
        } catch (Exception e) {
            log.error("Failed to serialize {}", eventType, e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
