package iuh.fit.se.modules.order.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.order.adapter.outbound.persistence.JpaOrderOutboxRepository;
import iuh.fit.se.modules.order.application.event.OrderCreatedIntegrationEvent;
import iuh.fit.se.modules.order.domain.OrderOutboxEvent;
import iuh.fit.se.modules.order.domain.OrderOutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxPublisherJob {

    private final JpaOrderOutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // 5 seconds
    @Transactional
    public void publishEvents() {
        List<OrderOutboxEvent> pendingEvents = outboxRepository.findByStatus(OrderOutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) return;

        log.info("Found {} pending order outbox events to publish", pendingEvents.size());

        for (OrderOutboxEvent event : pendingEvents) {
            try {
                Object integrationEvent = null;
                String type = event.getEventType();

                if ("OrderCreatedIntegrationEvent".equals(type)) {
                    integrationEvent = objectMapper.readValue(event.getPayload(), OrderCreatedIntegrationEvent.class);
                }

                if (integrationEvent != null) {
                    eventPublisher.publishEvent(integrationEvent);
                    event.markPublished();
                    outboxRepository.save(event);
                    log.info("Published order integration event: {} - {}", event.getId(), type);
                } else {
                    log.warn("Unknown event type for order outbox: {}", type);
                }

            } catch (Exception e) {
                log.error("Failed to publish order outbox event: {}", event.getId(), e);
                event.markFailed();
                outboxRepository.save(event);
            }
        }
    }
}
