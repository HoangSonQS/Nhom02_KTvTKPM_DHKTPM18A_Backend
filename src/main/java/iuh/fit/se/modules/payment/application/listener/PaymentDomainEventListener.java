package iuh.fit.se.modules.payment.application.listener;

import iuh.fit.se.modules.payment.domain.event.PaymentSuccessIntegrationEvent;
import iuh.fit.se.modules.payment.application.port.out.PaymentEventPort;
import iuh.fit.se.modules.payment.domain.Payment;
import iuh.fit.se.modules.payment.domain.event.PaymentSuccessDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentDomainEventListener {

    private final PaymentEventPort paymentEventPort;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePaymentSuccess(PaymentSuccessDomainEvent domainEvent) {
        log.info("Handling PaymentSuccessDomainEvent for order: {}", domainEvent.getPayment().getOrderId());
        
        Payment payment = domainEvent.getPayment();
        
        PaymentSuccessIntegrationEvent integrationEvent = new PaymentSuccessIntegrationEvent(
                java.util.UUID.randomUUID().toString(),
                "PAY-" + payment.getOrderId(),
                payment.getOrderId(),
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getPaymentMethod()
        );

        paymentEventPort.publishPaymentSuccess(integrationEvent);
    }
}
