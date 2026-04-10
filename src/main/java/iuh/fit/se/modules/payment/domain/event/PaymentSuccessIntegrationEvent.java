package iuh.fit.se.modules.payment.domain.event;

import java.math.BigDecimal;

/**
 * Integration Event: PaymentSuccessIntegrationEvent (PUBLIC)
 */
public record PaymentSuccessIntegrationEvent(
    String id,
    String correlationId,
    Long orderId,
    String transactionId,
    BigDecimal amount,
    String paymentMethod
) {}
