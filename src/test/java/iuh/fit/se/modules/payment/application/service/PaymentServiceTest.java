package iuh.fit.se.modules.payment.application.service;

import iuh.fit.se.modules.payment.application.port.out.OrderPaymentPort;
import iuh.fit.se.modules.payment.application.port.out.PaymentPersistencePort;
import iuh.fit.se.modules.payment.domain.Payment;
import iuh.fit.se.modules.payment.domain.PaymentStatus;
import iuh.fit.se.modules.payment.domain.event.PaymentSuccessDomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentPersistencePort paymentPersistencePort;
    @Mock private OrderPaymentPort orderPaymentPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PaymentService paymentService;

    private Map<String, String> vnpParams;
    private OrderPaymentPort.OrderPaymentDto orderDto;

    @BeforeEach
    void setUp() {
        PaymentService rawService = new PaymentService(paymentPersistencePort, orderPaymentPort, eventPublisher);
        paymentService = spy(rawService);

        vnpParams = new HashMap<>();
        vnpParams.put("vnp_ResponseCode", "00");
        vnpParams.put("vnp_TxnRef", "1");
        vnpParams.put("vnp_Amount", "20000000"); // 200,000 VND
        vnpParams.put("vnp_TransactionNo", "vnp-12345");
        vnpParams.put("vnp_SecureHash", "mock-hash");

        lenient().doReturn(true).when(paymentService).verifyChecksum(anyMap());

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
        verify(orderPaymentPort).updateOrderPaid(1L);
        verify(eventPublisher).publishEvent(any(PaymentSuccessDomainEvent.class));
    }

    @Test
    void testProcessVnpayIpn_existingTransaction_repairsPendingOrderStatus() {
        // Given
        Payment existingPayment = Payment.builder()
                .orderId(1L)
                .amount(new BigDecimal("200000"))
                .paymentMethod("VNPAY")
                .status(PaymentStatus.SUCCESS)
                .transactionId("vnp-12345")
                .build();
        when(orderPaymentPort.findOrderForPayment(1L)).thenReturn(Optional.of(orderDto));
        when(paymentPersistencePort.findByTransactionId("vnp-12345")).thenReturn(Optional.of(existingPayment));

        // When
        String response = paymentService.processVnpayIpn(vnpParams);

        // Then
        assertTrue(response.contains("Already confirmed"));
        verify(orderPaymentPort).updateOrderPaid(1L);
        verify(paymentPersistencePort, never()).save(any(Payment.class));
        verify(eventPublisher, never()).publishEvent(any());
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

    @Test
    void createPaymentUrl_whenJvmRunsUtc_usesVietnamTimezoneForVnpayDates() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            ReflectionTestUtils.setField(paymentService, "tmnCode", "TEST_TMNCODE");
            ReflectionTestUtils.setField(paymentService, "hashSecret", "TEST_HASH_SECRET");
            ReflectionTestUtils.setField(paymentService, "returnUrl", "https://api.test/api/v1/payments/vnpay-return");

            orderDto.setStatus("PENDING");
            orderDto.setCustomerId(5L);
            orderDto.setDiscountAmount(BigDecimal.ZERO);
            when(orderPaymentPort.findOrderForPayment(1L)).thenReturn(Optional.of(orderDto));

            LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusSeconds(2);
            String paymentUrl = paymentService.createPaymentUrl(1L, 5L, "127.0.0.1");
            LocalDateTime after = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(2);

            Map<String, String> queryParams = parseQuery(paymentUrl);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime createDate = LocalDateTime.parse(queryParams.get("vnp_CreateDate"), formatter);
            LocalDateTime expireDate = LocalDateTime.parse(queryParams.get("vnp_ExpireDate"), formatter);

            assertFalse(createDate.isBefore(before));
            assertFalse(createDate.isAfter(after));
            assertEquals(createDate.plusMinutes(15), expireDate);
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    private Map<String, String> parseQuery(String url) {
        Map<String, String> params = new HashMap<>();
        String query = URI.create(url).getRawQuery();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            params.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : ""
            );
        }
        return params;
    }
}
