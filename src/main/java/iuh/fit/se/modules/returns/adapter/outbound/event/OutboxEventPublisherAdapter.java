package iuh.fit.se.modules.returns.adapter.outbound.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.returns.adapter.outbound.persistence.JpaReturnOutboxRepository;
import iuh.fit.se.modules.returns.domain.event.ReturnIntegrationEvents.*;
import iuh.fit.se.modules.returns.application.port.out.ReturnEventPort;
import iuh.fit.se.modules.returns.domain.ReturnOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("returnOutboxEventPublisherAdapter")
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherAdapter implements ReturnEventPort {

    private final JpaReturnOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publishReturnCreated(ReturnRequestCreatedIntegrationEvent event) {
        saveEvent("ReturnRequestCreatedIntegrationEvent", event);
    }

    @Override
    public void publishReturnApproved(ReturnRequestApprovedIntegrationEvent event) {
        saveEvent("ReturnRequestApprovedIntegrationEvent", event);
    }

    @Override
    public void publishReturnReceived(ReturnRequestReceivedIntegrationEvent event) {
        saveEvent("ReturnRequestReceivedIntegrationEvent", event);
    }

    @Override
    public void publishReturnRefunded(ReturnRequestRefundedIntegrationEvent event) {
        saveEvent("ReturnRequestRefundedIntegrationEvent", event);
    }

    @Override
    public void publishReturnRejected(ReturnRequestRejectedIntegrationEvent event) {
        saveEvent("ReturnRequestRejectedIntegrationEvent", event);
    }

    private void saveEvent(String type, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            ReturnOutboxEvent outboxEvent = ReturnOutboxEvent.create(type, payload);
            outboxRepository.save(outboxEvent);
            log.info("Saved {} to outbox: {}", type, outboxEvent.getId());
        } catch (Exception e) {
            log.error("Failed to serialize {}", type, e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}
