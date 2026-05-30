package iuh.fit.se.modules.payment.application.service;

import iuh.fit.se.modules.payment.application.port.in.PaymentUseCase;
import iuh.fit.se.modules.payment.application.port.out.OrderPaymentPort;
import iuh.fit.se.modules.payment.application.port.out.PaymentPersistencePort;
import iuh.fit.se.modules.payment.domain.Payment;
import iuh.fit.se.modules.payment.domain.PaymentStatus;
import iuh.fit.se.modules.payment.domain.event.PaymentSuccessDomainEvent;
import iuh.fit.se.shared.event.payment.PaymentFailedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements PaymentUseCase {

    private final PaymentPersistencePort paymentPersistencePort;
    private final OrderPaymentPort orderPaymentPort;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${vnpay.tmncode}")
    private String tmnCode;

    @Value("${vnpay.hashsecret}")
    private String hashSecret;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Override
    @Transactional
    public String processVnpayIpn(Map<String, String> params) {
        return handlePaymentCallback(params);
    }

    /**
     * Logic xử lý kết quả thanh toán dùng chung cho cả IPN và Return URL (để test local)
     */
    @Transactional
    public String handlePaymentCallback(Map<String, String> params) {
        log.info("Processing Payment Callback: {}", params);
        
        // 1. Verify Checksum
        if (!verifyChecksum(params)) {
            log.error("VNPay Checksum Verification Failed!");
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
        }

        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_TransactionNo = params.get("vnp_TransactionNo");
        
        String amountStr = params.get("vnp_Amount");
        if (amountStr == null) return "{\"RspCode\":\"99\",\"Message\":\"Invalid amount\"}";
        BigDecimal amount = new BigDecimal(amountStr).divide(new BigDecimal(100));

        log.info("Processing Payment Callback for Order {}. ResponseCode: {}", vnp_TxnRef, vnp_ResponseCode);

        Long orderId;
        try {
            // Extract orderId from vnp_TxnRef (format: orderId_timestamp)
            if (vnp_TxnRef.contains("_")) {
                orderId = Long.parseLong(vnp_TxnRef.split("_")[0]);
            } else {
                orderId = Long.parseLong(vnp_TxnRef);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid vnp_TxnRef format: {}", vnp_TxnRef);
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }
        
        Optional<OrderPaymentPort.OrderPaymentDto> orderOpt = orderPaymentPort.findOrderForPayment(orderId);
        if (orderOpt.isEmpty()) {
            log.error("Payment Error: Order {} not found", orderId);
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }
        OrderPaymentPort.OrderPaymentDto order = orderOpt.get();

        if ("CONFIRMED".equals(order.getStatus())) {
            log.info("Payment Callback: Order {} already paid (status=CONFIRMED).", orderId);
            return "{\"RspCode\":\"00\",\"Message\":\"Already confirmed\"}";
        }

        if ("CANCELLED".equals(order.getStatus())) {
            log.info("Payment Callback: Order {} already cancelled.", orderId);
            return "{\"RspCode\":\"00\",\"Message\":\"Order already cancelled\"}";
        }

        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal expectedFinalAmount = order.getTotalAmount().subtract(discount);

        if (expectedFinalAmount.compareTo(amount) != 0) {
            log.error("Payment Error: Amount mismatch for order {}. Expected {}, got {}", orderId, expectedFinalAmount, amount);
            return "{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}";
        }

        if (!"00".equals(vnp_ResponseCode)) {
            log.warn("Payment Callback: Payment failed for Order {} (vnp_ResponseCode={})", orderId, vnp_ResponseCode);
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .amount(amount)
                    .paymentMethod("VNPAY")
                    .status(PaymentStatus.FAILED)
                    .resultData(params.toString())
                    .build();
            paymentPersistencePort.save(payment);
            eventPublisher.publishEvent(PaymentFailedIntegrationEvent.of(
                    orderId,
                    order.getCustomerId(),
                    amount,
                    "VNPAY",
                    vnp_ResponseCode,
                    "PAY-" + orderId
            ));
            return "{\"RspCode\":\"00\",\"Message\":\"Confirm success\"}";
        }

        // Idempotency: check by transactionId to prevent duplicate Payment records
        // (e.g. IPN arrives after vnpay-return already processed the same vnp_TransactionNo)
        if (vnp_TransactionNo != null) {
            Optional<Payment> existing = paymentPersistencePort.findByTransactionId(vnp_TransactionNo);
            if (existing.isPresent()) {
                if (!"CONFIRMED".equals(order.getStatus())) {
                    orderPaymentPort.updateOrderPaid(orderId);
                    log.info("Payment Callback: Order {} status repaired to CONFIRMED for existing transaction {}.", orderId, vnp_TransactionNo);
                }
                log.info("Payment Callback: TransactionId {} already recorded, skipping duplicate.", vnp_TransactionNo);
                return "{\"RspCode\":\"00\",\"Message\":\"Already confirmed\"}";
            }
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .paymentMethod("VNPAY")
                .status(PaymentStatus.SUCCESS)
                .transactionId(vnp_TransactionNo)
                .resultData(params.toString())
                .build();
        
        paymentPersistencePort.save(payment);
        orderPaymentPort.updateOrderPaid(orderId);
        eventPublisher.publishEvent(PaymentSuccessDomainEvent.of(payment, order.getCustomerId(), order.getRequestId()));

        log.info("Payment Callback: Successfully updated status for Order {}.", orderId);
        return "{\"RspCode\":\"00\",\"Message\":\"Confirm success\"}";
    }

    protected boolean verifyChecksum(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        if (vnp_SecureHash == null) return false;

        Map<String, String> hashParams = new HashMap<>(params);
        hashParams.remove("vnp_SecureHash");
        hashParams.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(hashParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData20 = new StringBuilder();
        StringBuilder hashDataPlus = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (!fieldName.startsWith("vnp_")) {
                continue;
            }
            String fieldValue = hashParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                if (hashData20.length() > 0) {
                    hashData20.append('&');
                    hashDataPlus.append('&');
                }
                hashData20.append(fieldName).append('=').append(urlEncode(fieldValue));
                try {
                    hashDataPlus.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    hashDataPlus.append(fieldName).append('=').append(fieldValue);
                }
            }
        }

        String calculatedHash20 = iuh.fit.se.modules.payment.adapter.outbound.vnpay.VnPayUtils.hmacSHA512(hashSecret, hashData20.toString());
        String calculatedHashPlus = iuh.fit.se.modules.payment.adapter.outbound.vnpay.VnPayUtils.hmacSHA512(hashSecret, hashDataPlus.toString());
        
        log.info("VNPay Callback HashData (%20): {}", hashData20);
        log.info("VNPay Callback HashData (+): {}", hashDataPlus);
        log.info("VNPay Callback CalculatedHash (%20): {}", calculatedHash20);
        log.info("VNPay Callback CalculatedHash (+): {}", calculatedHashPlus);
        log.info("VNPay Callback ReceivedHash: {}", vnp_SecureHash);
        
        if (calculatedHash20.equalsIgnoreCase(vnp_SecureHash)) {
            log.info("VNPay Checksum matches using %20 encoding");
            return true;
        }
        
        if (calculatedHashPlus.equalsIgnoreCase(vnp_SecureHash)) {
            log.info("VNPay Checksum matches using + encoding");
            return true;
        }
        
        return false;
    }

    private String urlEncode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Override
    public String createPaymentUrl(Long orderId, Long requesterId, String ipAddress) {
        log.info("Creating VNPay payment URL for order: {} from userId: {} IP: {}", orderId, requesterId, ipAddress);

        OrderPaymentPort.OrderPaymentDto order = orderPaymentPort.findOrderForPayment(orderId)
                .orElseThrow(() -> new iuh.fit.se.shared.exception.AppException(iuh.fit.se.shared.exception.ErrorCode.ORD_NOT_FOUND));

        // Ownership check: ADMIN (requesterId == null means bypass) or customer must own the order
        if (requesterId != null && !requesterId.equals(order.getCustomerId())) {
            throw new iuh.fit.se.shared.exception.AppException(iuh.fit.se.shared.exception.ErrorCode.ACCESS_DENIED, "Bạn không có quyền thanh toán đơn hàng này");
        }

        if ("CONFIRMED".equals(order.getStatus())) {
            throw new iuh.fit.se.shared.exception.AppException(iuh.fit.se.shared.exception.ErrorCode.ORD_ALREADY_PAID);
        } else if (!"PENDING".equals(order.getStatus())) {
            throw new iuh.fit.se.shared.exception.AppException(iuh.fit.se.shared.exception.ErrorCode.ORD_INVALID_STATUS, "Đơn hàng đang ở trạng thái " + order.getStatus() + ", không thể thanh toán");
        }

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = "Thanh toan don hang SEBook: " + orderId;
        String vnp_OrderType = "other";
        
        // Use orderId + timestamp to make vnp_TxnRef unique for each attempt
        String vnp_TxnRef = orderId + "_" + System.currentTimeMillis();
        
        String vnp_IpAddr = ipAddress;
        if (vnp_IpAddr.equals("0:0:0:0:0:0:0:1")) {
            vnp_IpAddr = "127.0.0.1";
        }
        String vnp_TmnCode = tmnCode;

        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = order.getTotalAmount().subtract(discount);
        long amount = finalAmount.multiply(new BigDecimal(100)).longValue();
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        TimeZone vietnamTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(vietnamTimeZone);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(vietnamTimeZone);
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Sort and build hash data/query string
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (hashData.length() > 0) {
                    hashData.append('&');
                    query.append('&');
                }
                
                String encodedValue = urlEncode(fieldValue);
                
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(encodedValue);
                
                // Build query string
                query.append(urlEncode(fieldName));
                query.append('=');
                query.append(encodedValue);
            }
        }
        
        String queryUrl = query.toString();
        String vnp_SecureHash = iuh.fit.se.modules.payment.adapter.outbound.vnpay.VnPayUtils.hmacSHA512(hashSecret, hashData.toString());
        log.info("VNPay HashData: {}", hashData);
        log.info("VNPay SecureHash: {}", vnp_SecureHash);
        
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return iuh.fit.se.modules.payment.adapter.outbound.vnpay.VnPayUtils.VNP_PAYURL + "?" + queryUrl;
    }

    @Override
    @Transactional
    public void processRefund(Long orderId, BigDecimal amount, String returnRequestId) {
        log.info("Processing refund for Order {}. Amount: {}. ReturnRequest: {}", orderId, amount, returnRequestId);

        // 1. Find existing payment record
        Payment payment = paymentPersistencePort.findByOrderId(orderId)
                .orElseThrow(() -> new iuh.fit.se.shared.exception.AppException(iuh.fit.se.shared.exception.ErrorCode.ORD_NOT_FOUND, "Không tìm thấy giao dịch thanh toán cho đơn hàng: " + orderId));

        // 2. Simulate VNPay Refund API call
        log.info("Calling VNPay Refund API (Simulated) for transaction: {}", payment.getTransactionId());
        
        // In reality, this would be a RestTemplate call to VNPay
        boolean refundSuccess = true; 

        if (refundSuccess) {
            // 3. Update payment status to REFUNDED
            payment.markRefunded(amount, returnRequestId);
            paymentPersistencePort.save(payment);
            log.info("Refund successful for Order {}. Payment updated.", orderId);
        } else {
            log.error("VNPay Refund API failed for Order {}", orderId);
            throw new iuh.fit.se.shared.exception.AppException(iuh.fit.se.shared.exception.ErrorCode.INTERNAL_ERROR, "Lỗi khi gọi API hoàn tiền VNPay");
        }
    }
}
