package iuh.fit.se.modules.payment.domain;

import iuh.fit.se.shared.domain.BaseEvent;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * PaymentSuccessEvent — Sự kiện thanh toán thành công (VNPAY IPN).
 */
@Getter
@SuperBuilder
public class PaymentSuccessEvent extends BaseEvent {
    private final Long orderId;
    private final String transactionId;
    private final BigDecimal amount;
    private final String paymentMethod;

    public static PaymentSuccessEvent create(Payment payment) {
        return PaymentSuccessEvent.builder()
                .correlationId("PAY-" + payment.getOrderId() + "-" + System.currentTimeMillis()) // Tạm thời dùng pattern này nếu Payment không lưu requestId
                .eventType("PAYMENT_SUCCESS")
                .orderId(payment.getOrderId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .build();
    }
}
