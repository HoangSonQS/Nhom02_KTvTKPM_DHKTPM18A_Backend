package iuh.fit.se.modules.returns.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.returns.application.port.out.ReturnOutboxPersistencePort;
import iuh.fit.se.modules.returns.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnOutboxPublisherJob {

    private final ReturnOutboxPersistencePort outboxPort;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // 5 seconds
    public void publishEvents() {
        List<ReturnOutboxEvent> pendingEvents = outboxPort.findPendingEvents();
        if (pendingEvents.isEmpty()) return;

        log.info("Found {} pending return outbox events to publish", pendingEvents.size());

        for (ReturnOutboxEvent event : pendingEvents) {
            try {
                Object domainEvent = null;
                String type = event.getEventType();

                if ("ReturnRequestCreatedEvent".equals(type)) {
                    domainEvent = objectMapper.readValue(event.getPayload(), ReturnRequestCreatedEvent.class);
                } else if ("ReturnRequestApprovedEvent".equals(type)) {
                    domainEvent = objectMapper.readValue(event.getPayload(), ReturnRequestApprovedEvent.class);
                } else if ("ReturnRequestReceivedEvent".equals(type)) {
                    domainEvent = objectMapper.readValue(event.getPayload(), ReturnRequestReceivedEvent.class);
                } else if ("ReturnRequestRefundedEvent".equals(type)) {
                    domainEvent = objectMapper.readValue(event.getPayload(), ReturnRequestRefundedEvent.class);
                } else if ("ReturnRequestRejectedEvent".equals(type)) {
                    domainEvent = objectMapper.readValue(event.getPayload(), ReturnRequestRejectedEvent.class);
                }

                if (domainEvent != null) {
                    eventPublisher.publishEvent(domainEvent);
                    event.markPublished();
                    outboxPort.save(event);
                    log.info("Published return event: {} - {}", event.getId(), type);
                } else {
                    log.warn("Unknown event type: {}", type);
                }

            } catch (Exception e) {
                log.error("Failed to publish return outbox event: {}", event.getId(), e);
                event.markFailed();
                outboxPort.save(event);
            }
        }
    }
}
