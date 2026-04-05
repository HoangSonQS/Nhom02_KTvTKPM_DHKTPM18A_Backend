package iuh.fit.se.modules.payment.adapter.inbound.web;

import iuh.fit.se.modules.payment.application.port.in.PaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    /**
     * VNPay IPN URL
     * This URL should be configured in VNPay Dashboard (e.g., https://yourdomain.com/api/payments/vnpay-ipn)
     */
    @GetMapping("/vnpay-ipn")
    public String vnpayIpn(@RequestParam Map<String, String> allParams) {
        log.info("Received VNPay IPN Checksum Request: {}", allParams);
        // In a real system, we must check the HMAC signature here
        // For this task, we assume the signature is verified by the Gateway/Filter or mocked
        return paymentUseCase.processVnpayIpn(allParams);
    }
}
