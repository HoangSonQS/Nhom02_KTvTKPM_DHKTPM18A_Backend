package iuh.fit.se.modules.returns.application.listener;

import iuh.fit.se.modules.returns.domain.event.ReturnIntegrationEvents.*;
import iuh.fit.se.modules.returns.application.port.out.ReturnEventPort;
import iuh.fit.se.modules.returns.domain.ReturnRequest;
import iuh.fit.se.modules.returns.domain.event.ReturnDomainEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnDomainEventListener {

    private final ReturnEventPort returnEventPort;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReturnCreated(ReturnRequestCreatedDomainEvent event) {
        ReturnRequest request = event.getReturnRequest();
        ReturnRequestCreatedIntegrationEvent integrationEvent = new ReturnRequestCreatedIntegrationEvent(
                UUID.randomUUID().toString(),
                event.getCorrelationId(),
                request.getId(),
                request.getOrderId(),
                request.getCustomerId()
        );
        returnEventPort.publishReturnCreated(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReturnApproved(ReturnRequestApprovedDomainEvent event) {
        ReturnRequest request = event.getReturnRequest();
        ReturnRequestApprovedIntegrationEvent integrationEvent = new ReturnRequestApprovedIntegrationEvent(
                UUID.randomUUID().toString(),
                event.getCorrelationId(),
                request.getId(),
                request.getOrderId()
        );
        returnEventPort.publishReturnApproved(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReturnReceived(ReturnRequestReceivedDomainEvent event) {
        ReturnRequest request = event.getReturnRequest();
        ReturnRequestReceivedIntegrationEvent integrationEvent = new ReturnRequestReceivedIntegrationEvent(
                UUID.randomUUID().toString(),
                event.getCorrelationId(),
                request.getId(),
                request.getOrderId(),
                request.getItems().stream()
                        .map(item -> ReturnedItemCondition.builder()
                                .bookId(item.getBookId())
                                .quantity(item.getQuantity())
                                .condition(item.getCondition())
                                .build())
                        .collect(Collectors.toList())
        );
        returnEventPort.publishReturnReceived(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReturnRefunded(ReturnRequestRefundedDomainEvent event) {
        ReturnRequest request = event.getReturnRequest();
        ReturnRequestRefundedIntegrationEvent integrationEvent = new ReturnRequestRefundedIntegrationEvent(
                UUID.randomUUID().toString(),
                event.getCorrelationId(),
                request.getId(),
                request.getOrderId(),
                request.getRefundAmount()
        );
        returnEventPort.publishReturnRefunded(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReturnRejected(ReturnRequestRejectedDomainEvent event) {
        ReturnRequest request = event.getReturnRequest();
        ReturnRequestRejectedIntegrationEvent integrationEvent = new ReturnRequestRejectedIntegrationEvent(
                UUID.randomUUID().toString(),
                event.getCorrelationId(),
                request.getId(),
                request.getOrderId(),
                event.getReason()
        );
        returnEventPort.publishReturnRejected(integrationEvent);
    }
}
