package iuh.fit.se.modules.payment.adapter.inbound.web;

import iuh.fit.se.modules.payment.adapter.outbound.vnpay.VnPayUtils;
import iuh.fit.se.modules.payment.application.port.in.PaymentUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * VNPay IPN URL.
     * This URL should be configured in VNPay Dashboard, for example:
     * https://yourdomain.com/api/v1/payments/vnpay-ipn
     */
    @GetMapping("/vnpay-ipn")
    public String vnpayIpn(@RequestParam Map<String, String> allParams) {
        log.info("Received VNPay IPN Checksum Request: {}", allParams);
        return paymentUseCase.processVnpayIpn(allParams);
    }

    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    @PostMapping("/create-payment-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPaymentUrl(
            @RequestParam Long orderId,
            HttpServletRequest request
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        String ipAddress = VnPayUtils.getIpAddress(request);
        String paymentUrl = paymentUseCase.createPaymentUrl(orderId, requesterId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(Map.of("paymentUrl", paymentUrl)));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> allParams) {
        log.info("Received VNPay Return Callback: {}", allParams);
        log.info("Frontend URL configured: {}", frontendUrl);

        try {
            paymentUseCase.handlePaymentCallback(allParams);
        } catch (Exception e) {
            log.error("Payment processing error: {}", e.getMessage());
        }

        // Always redirect to /payment/result — the frontend PaymentResultPage handles
        // all cases (success, cancelled, failed) based on vnp_ResponseCode.
        String responseCode = allParams.get("vnp_ResponseCode");
        String paymentStatus;
        if ("00".equals(responseCode)) {
            paymentStatus = "success";
        } else if ("24".equals(responseCode)) {
            paymentStatus = "cancelled";
        } else {
            paymentStatus = "failed";
        }

        URI redirectUri = buildFrontendRedirect("/payment/result", allParams)
                .queryParam("paymentStatus", paymentStatus)
                .encode()
                .build()
                .toUri();
        log.info("Redirecting user to frontend: {}", redirectUri);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    private UriComponentsBuilder buildFrontendRedirect(String path, Map<String, String> vnpayParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(frontendUrl).path(path);
        vnpayParams.forEach(builder::queryParam);
        return builder;
    }
}
