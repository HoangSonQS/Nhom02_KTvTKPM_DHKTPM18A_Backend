package iuh.fit.se.modules.payment.application.port.out;

import iuh.fit.se.modules.payment.domain.event.PaymentSuccessIntegrationEvent;

public interface PaymentEventPort {
    void publishPaymentSuccess(PaymentSuccessIntegrationEvent event);
}
