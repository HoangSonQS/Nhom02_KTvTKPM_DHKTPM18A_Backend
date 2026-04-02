package iuh.fit.se.modules.payment.application.service;

import iuh.fit.se.modules.payment.application.port.in.PaymentUseCase;
import iuh.fit.se.modules.payment.application.port.out.OrderPaymentPort;
import iuh.fit.se.modules.payment.application.port.out.PaymentPersistencePort;
import iuh.fit.se.modules.payment.domain.Payment;
import iuh.fit.se.modules.payment.domain.PaymentStatus;
import iuh.fit.se.modules.payment.domain.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements PaymentUseCase {

    private final PaymentPersistencePort paymentPersistencePort;
    private final OrderPaymentPort orderPaymentPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public String processVnpayIpn(Map<String, String> params) {
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TxnRef = params.get("vnp_TxnRef"); // Assumed to be Order ID
        String vnp_TransactionNo = params.get("vnp_TransactionNo");
        
        String amountStr = params.get("vnp_Amount");
        if (amountStr == null) return "{\"RspCode\":\"99\",\"Message\":\"Invalid amount\"}";
        BigDecimal amount = new BigDecimal(amountStr).divide(new BigDecimal(100));

        log.info("Processing VNPay IPN for Order {}. ResponseCode: {}", vnp_TxnRef, vnp_ResponseCode);

        Long orderId;
        try {
            orderId = Long.parseLong(vnp_TxnRef);
        } catch (NumberFormatException e) {
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }
        
        // 1. Check order existence via internal port
        Optional<OrderPaymentPort.OrderPaymentDto> orderOpt = orderPaymentPort.findOrderForPayment(orderId);
        if (orderOpt.isEmpty()) {
            log.error("IPN Error: Order {} not found", orderId);
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }
        OrderPaymentPort.OrderPaymentDto order = orderOpt.get();

        // 2. Check if already confirmed (Idempotency)
        if ("PAID".equals(order.getStatus())) {
            log.info("IPN: Order {} already paid. Acknowledging VNPay.", orderId);
            return "{\"RspCode\":\"00\",\"Message\":\"Already confirmed\"}";
        }

        // 3. Check Order state - if CANCELLED, still return 00 but don't process
        if ("CANCELLED".equals(order.getStatus())) {
            log.warn("IPN: Order {} was already CANCELLED. Acknowledging VNPay but skipping process.", orderId);
            return "{\"RspCode\":\"00\",\"Message\":\"Order already cancelled\"}";
        }

        // 4. Check amount compatibility
        if (order.getTotalAmount().compareTo(amount) != 0) {
            log.error("IPN Error: Amount mismatch for order {}. Expected {}, got {}", orderId, order.getTotalAmount(), amount);
            return "{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}";
        }

        // 5. Handle VNPay response code
        if (!"00".equals(vnp_ResponseCode)) {
            log.warn("IPN: Payment failed for Order {} (vnp_ResponseCode={})", orderId, vnp_ResponseCode);
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .amount(amount)
                    .paymentMethod("VNPAY")
                    .status(PaymentStatus.FAILED)
                    .resultData(params.toString())
                    .build();
            paymentPersistencePort.save(payment);
            return "{\"RspCode\":\"00\",\"Message\":\"Confirm success\"}";
        }

        // 6. Success payment handling
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .paymentMethod("VNPAY")
                .status(PaymentStatus.SUCCESS)
                .transactionId(vnp_TransactionNo)
                .resultData(params.toString())
                .build();
        
        paymentPersistencePort.save(payment);

        // Emit success event - Order module will listen and finalize
        eventPublisher.publishEvent(PaymentSuccessEvent.create(payment));

        log.info("IPN: Successfully processed payment for Order {}. Event emitted.", orderId);
        return "{\"RspCode\":\"00\",\"Message\":\"Confirm success\"}";
    }
}
