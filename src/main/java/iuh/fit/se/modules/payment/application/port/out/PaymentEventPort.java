package iuh.fit.se.modules.payment.application.port.out;

import iuh.fit.se.shared.event.payment.PaymentSuccessIntegrationEvent;

public interface PaymentEventPort {
    void publishPaymentSuccess(PaymentSuccessIntegrationEvent event);
}
