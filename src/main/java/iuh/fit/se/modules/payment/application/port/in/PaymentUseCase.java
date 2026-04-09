package iuh.fit.se.modules.payment.application.port.in;

import java.util.Map;

public interface PaymentUseCase {
    String processVnpayIpn(Map<String, String> params);

    void processRefund(java.lang.Long orderId, java.math.BigDecimal amount, String returnRequestId);
}
