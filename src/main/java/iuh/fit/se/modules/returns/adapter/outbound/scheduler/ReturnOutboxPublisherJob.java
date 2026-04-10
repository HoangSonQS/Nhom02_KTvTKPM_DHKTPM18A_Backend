package iuh.fit.se.modules.returns.adapter.outbound.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.returns.domain.event.ReturnIntegrationEvents.*;
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
        if (pendingEvents.isEmpty())
            return;

        log.info("Found {} pending return outbox events to publish", pendingEvents.size());

        for (ReturnOutboxEvent event : pendingEvents) {
            try {
                Object integrationEvent = null;
                String type = event.getEventType();

                if ("ReturnRequestCreatedIntegrationEvent".equals(type)) {
                    integrationEvent = objectMapper.readValue(event.getPayload(),
                            ReturnRequestCreatedIntegrationEvent.class);
                } else if ("ReturnRequestApprovedIntegrationEvent".equals(type)) {
                    integrationEvent = objectMapper.readValue(event.getPayload(),
                            ReturnRequestApprovedIntegrationEvent.class);
                } else if ("ReturnRequestReceivedIntegrationEvent".equals(type)) {
                    integrationEvent = objectMapper.readValue(event.getPayload(),
                            ReturnRequestReceivedIntegrationEvent.class);
                } else if ("ReturnRequestRefundedIntegrationEvent".equals(type)) {
                    integrationEvent = objectMapper.readValue(event.getPayload(),
                            ReturnRequestRefundedIntegrationEvent.class);
                } else if ("ReturnRequestRejectedIntegrationEvent".equals(type)) {
                    integrationEvent = objectMapper.readValue(event.getPayload(),
                            ReturnRequestRejectedIntegrationEvent.class);
                }

                if (integrationEvent != null) {
                    eventPublisher.publishEvent(integrationEvent);
                    event.markPublished();
                    outboxPort.save(event);
                    log.info("Published return integration event: {} - {}", event.getId(), type);
                } else {
                    log.warn("Unknown integration event type: {}", type);
                }

            } catch (Exception e) {
                log.error("Failed to publish return outbox event: {}", event.getId(), e);
                event.markFailed();
                outboxPort.save(event);
            }
        }
    }
}
