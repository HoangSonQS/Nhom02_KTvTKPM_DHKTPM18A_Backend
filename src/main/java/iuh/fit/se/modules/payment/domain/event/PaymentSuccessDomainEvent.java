package iuh.fit.se.modules.payment.domain.event;

import iuh.fit.se.modules.payment.domain.Payment;
import iuh.fit.se.shared.domain.BaseEvent;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Domain Event: PaymentSuccessDomainEvent (Module-Internal)
 */
@Getter
@SuperBuilder
public class PaymentSuccessDomainEvent extends BaseEvent {
    private final Payment payment;

    public static PaymentSuccessDomainEvent of(Payment payment) {
        return PaymentSuccessDomainEvent.builder()
                .correlationId("PAY-" + payment.getOrderId())
                .eventType("PAYMENT_SUCCESS_DOMAIN")
                .payment(payment)
                .build();
    }
}
