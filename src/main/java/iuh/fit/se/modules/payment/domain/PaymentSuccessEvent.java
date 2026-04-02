package iuh.fit.se.modules.payment.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentSuccessEvent {
    private final Long orderId;
    private final String transactionId;
    private final BigDecimal amount;

    public static PaymentSuccessEvent create(Payment payment) {
        return PaymentSuccessEvent.builder()
                .orderId(payment.getOrderId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .build();
    }
}
