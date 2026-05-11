package iuh.fit.se.modules.payment.adapter.inbound.web;

import iuh.fit.se.modules.payment.application.port.in.PaymentUseCase;
import iuh.fit.se.modules.payment.adapter.outbound.vnpay.VnPayUtils;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
        return paymentUseCase.processVnpayIpn(allParams);
    }

    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    @PostMapping("/create-payment-url")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPaymentUrl(@RequestParam Long orderId, HttpServletRequest request) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        String ipAddress = VnPayUtils.getIpAddress(request);
        String paymentUrl = paymentUseCase.createPaymentUrl(orderId, requesterId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(Map.of("paymentUrl", paymentUrl)));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<String>> vnpayReturn(@RequestParam Map<String, String> allParams) {
        log.info("Received VNPay Return Callback: {}", allParams);
        
        // Gọi logic xử lý thanh toán để cập nhật database (đặc biệt quan trọng khi test ở localhost)
        boolean isSuccess = false;
        try {
            paymentUseCase.handlePaymentCallback(allParams);
            isSuccess = true;
        } catch (Exception e) {
            log.error("Payment processing error: {}", e.getMessage());
        }

        String responseCode = allParams.get("vnp_ResponseCode");
        if ("00".equals(responseCode) && isSuccess) {
            return ResponseEntity.ok(ApiResponse.success("Payment Success! You can close this tab and return to the app.", null));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Payment Failed or Cancelled. Error Code: " + responseCode));
        }
    }
}
