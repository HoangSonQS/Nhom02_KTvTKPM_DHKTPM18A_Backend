package iuh.fit.se.modules.logistics.adapter.outbound.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.logistics.application.port.out.LogisticsOutboxPersistencePort;
import iuh.fit.se.modules.logistics.domain.LogisticsOutboxEvent;
import iuh.fit.se.modules.logistics.domain.event.StockAdjustmentConfirmedEvent;
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
                if ("StockAdjustmentConfirmedEvent".equals(event.getEventType())) {
                    StockAdjustmentConfirmedEvent domainEvent = objectMapper.readValue(
                            event.getPayload(), 
                            StockAdjustmentConfirmedEvent.class
                    );
                    
                    // Publish to Spring context
                    eventPublisher.publishEvent(domainEvent);
                    
                    // Mark as published
                    event.markPublished();
                    outboxPort.save(event);
                    log.info("Published event: {} - {}", event.getId(), event.getEventType());
                }
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getId(), e);
                event.markFailed();
                outboxPort.save(event);
            }
        }
    }
}
