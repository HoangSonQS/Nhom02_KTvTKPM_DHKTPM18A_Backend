package iuh.fit.se.modules.payment.adapter.outbound.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.payment.adapter.outbound.persistence.JpaPaymentOutboxRepository;
import iuh.fit.se.modules.payment.domain.event.PaymentSuccessIntegrationEvent;
import iuh.fit.se.modules.payment.application.port.out.PaymentEventPort;
import iuh.fit.se.modules.payment.domain.PaymentOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("paymentOutboxEventPublisherAdapter")
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherAdapter implements PaymentEventPort {

    private final JpaPaymentOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publishPaymentSuccess(PaymentSuccessIntegrationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            PaymentOutboxEvent outboxEvent = PaymentOutboxEvent.create("PaymentSuccessIntegrationEvent", payload);
            outboxRepository.save(outboxEvent);
            log.info("Saved PaymentSuccessIntegrationEvent to outbox: {}", outboxEvent.getId());
        } catch (Exception e) {
            log.error("Failed to serialize PaymentSuccessIntegrationEvent", e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
