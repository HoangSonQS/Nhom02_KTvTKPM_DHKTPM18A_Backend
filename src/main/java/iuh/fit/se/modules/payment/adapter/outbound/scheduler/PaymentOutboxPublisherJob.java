package iuh.fit.se.modules.payment.adapter.outbound.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.payment.adapter.outbound.persistence.JpaPaymentOutboxRepository;
import iuh.fit.se.modules.payment.domain.event.PaymentSuccessIntegrationEvent;
import iuh.fit.se.modules.payment.domain.PaymentOutboxEvent;
import iuh.fit.se.modules.payment.domain.PaymentOutboxStatus;
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
public class PaymentOutboxPublisherJob {

    private final JpaPaymentOutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // 5 seconds
    @Transactional
    public void publishEvents() {
        List<PaymentOutboxEvent> pendingEvents = outboxRepository.findByStatus(PaymentOutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) return;

        log.info("Found {} pending payment outbox events to publish", pendingEvents.size());

        for (PaymentOutboxEvent event : pendingEvents) {
            try {
                Object integrationEvent = null;
                String type = event.getEventType();

                if ("PaymentSuccessIntegrationEvent".equals(type)) {
                    integrationEvent = objectMapper.readValue(event.getPayload(), PaymentSuccessIntegrationEvent.class);
                }

                if (integrationEvent != null) {
                    eventPublisher.publishEvent(integrationEvent);
                    event.markPublished();
                    outboxRepository.save(event);
                    log.info("Published payment integration event: {} - {}", event.getId(), type);
                } else {
                    log.warn("Unknown event type for payment outbox: {}", type);
                }

            } catch (Exception e) {
                log.error("Failed to publish payment outbox event: {}", event.getId(), e);
                event.markFailed();
                outboxRepository.save(event);
            }
        }
    }
}
