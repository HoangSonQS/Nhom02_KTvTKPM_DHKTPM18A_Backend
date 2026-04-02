package iuh.fit.se.modules.payment.application.service;

import iuh.fit.se.modules.payment.application.port.out.OrderPaymentPort;
import iuh.fit.se.modules.payment.application.port.out.PaymentPersistencePort;
import iuh.fit.se.modules.payment.domain.Payment;
import iuh.fit.se.modules.payment.domain.PaymentStatus;
import iuh.fit.se.modules.payment.domain.PaymentSuccessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentPersistencePort paymentPersistencePort;
    @Mock private OrderPaymentPort orderPaymentPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private PaymentService paymentService;

    private Map<String, String> vnpParams;
    private OrderPaymentPort.OrderPaymentDto orderDto;

    @BeforeEach
    void setUp() {
        vnpParams = new HashMap<>();
        vnpParams.put("vnp_ResponseCode", "00");
        vnpParams.put("vnp_TxnRef", "1");
        vnpParams.put("vnp_Amount", "20000000"); // 200,000 VND
        vnpParams.put("vnp_TransactionNo", "vnp-12345");

        orderDto = OrderPaymentPort.OrderPaymentDto.builder()
                .orderId(1L)
                .totalAmount(new BigDecimal("200000"))
                .status("UNPAID")
                .build();
    }

    @Test
    void testProcessVnpayIpn_Success() {
        // Given
        when(orderPaymentPort.findOrderForPayment(1L)).thenReturn(Optional.of(orderDto));

        // When
        String response = paymentService.processVnpayIpn(vnpParams);

        // Then
        assertTrue(response.contains("RspCode\":\"00"));
        verify(paymentPersistencePort).save(any(Payment.class));
        verify(eventPublisher).publishEvent(any(PaymentSuccessEvent.class));
    }

    @Test
    void testProcessVnpayIpn_AmountMismatch() {
        // Given
        vnpParams.put("vnp_Amount", "10000000"); // 100,000 VND (Mismatch)
        when(orderPaymentPort.findOrderForPayment(1L)).thenReturn(Optional.of(orderDto));

        // When
        String response = paymentService.processVnpayIpn(vnpParams);

        // Then
        assertTrue(response.contains("RspCode\":\"04")); // Invalid amount
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testProcessVnpayIpn_OrderAlreadyCancelled() {
        // Given
        orderDto.setStatus("CANCELLED");
        when(orderPaymentPort.findOrderForPayment(1L)).thenReturn(Optional.of(orderDto));

        // When
        String response = paymentService.processVnpayIpn(vnpParams);

        // Then
        assertTrue(response.contains("RspCode\":\"00")); // Still success 00 per VNPay requirement
        assertTrue(response.contains("already cancelled"));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testProcessVnpayIpn_PaymentFailedAtVnpay() {
        // Given
        vnpParams.put("vnp_ResponseCode", "99"); // Failed code
        when(orderPaymentPort.findOrderForPayment(1L)).thenReturn(Optional.of(orderDto));

        // When
        String response = paymentService.processVnpayIpn(vnpParams);

        // Then
        assertTrue(response.contains("RspCode\":\"00")); // Confirm success to VNPay
        verify(paymentPersistencePort).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
        verify(eventPublisher, never()).publishEvent(any());
    }
}
