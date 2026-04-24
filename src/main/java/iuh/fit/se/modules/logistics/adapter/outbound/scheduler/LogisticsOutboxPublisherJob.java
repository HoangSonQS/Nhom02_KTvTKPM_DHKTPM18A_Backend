package iuh.fit.se.modules.logistics.adapter.outbound.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.logistics.application.port.out.LogisticsOutboxPersistencePort;
import iuh.fit.se.modules.logistics.domain.LogisticsOutboxEvent;
import iuh.fit.se.shared.event.logistics.StockAdjustmentIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogisticsOutboxPublisherJob {

    private final LogisticsOutboxPersistencePort outboxPort;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // 5 giây chạy 1 lần
    public void publishEvents() {
        List<LogisticsOutboxEvent> pendingEvents = outboxPort.findPendingEvents();
        if (pendingEvents.isEmpty()) return;

        log.info("Found {} pending outbox events to publish", pendingEvents.size());

        for (LogisticsOutboxEvent event : pendingEvents) {
            try {
                if ("StockAdjustmentIntegrationEvent".equals(event.getEventType())) {
                    StockAdjustmentIntegrationEvent integrationEvent = objectMapper.readValue(
                            event.getPayload(),
                            StockAdjustmentIntegrationEvent.class
                    );
                    eventPublisher.publishEvent(integrationEvent);
                    event.markPublished();
                    outboxPort.save(event);
                    log.info("Published integration event: {} - {}", event.getId(), event.getEventType());
                } else {
                    log.warn("Unknown event type for outbox event {}: {}", event.getId(), event.getEventType());
                    event.markFailed();
                    outboxPort.save(event);
                }
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getId(), e);
                event.markFailed();
                outboxPort.save(event);
            }
        }
    }
}
