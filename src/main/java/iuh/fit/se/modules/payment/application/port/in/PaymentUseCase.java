package iuh.fit.se.modules.payment.application.port.in;

import java.util.Map;

public interface PaymentUseCase {
    String processVnpayIpn(Map<String, String> params);
    
    String handlePaymentCallback(Map<String, String> params);

    String createPaymentUrl(Long orderId, Long requesterId, String ipAddress);

    void switchPendingVnpayOrderToCod(Long orderId, Long requesterId);

    void processRefund(java.lang.Long orderId, java.math.BigDecimal amount, String returnRequestId);
}
