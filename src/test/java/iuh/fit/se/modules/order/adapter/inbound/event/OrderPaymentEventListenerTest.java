package iuh.fit.se.modules.order.adapter.inbound.event;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.order.application.port.out.PromotionPort;
import iuh.fit.se.modules.payment.domain.event.PaymentSuccessIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentEventListenerTest {

    @Mock private OrderInternalUseCase orderUseCase;
    @Mock private PromotionPort promotionPort;

    @InjectMocks private OrderPaymentEventListener listener;

    @Test
    void testHandlePaymentSuccessEvent() {
        // Given
        Long orderId = 1L;
        String requestId = "req-123";
        PaymentSuccessIntegrationEvent event = new PaymentSuccessIntegrationEvent(
                "evt-123",
                "PAY-" + orderId,
                orderId,
                "vnpay-tx-123",
                new BigDecimal("200000"),
                "VNPAY"
        );

        OrderInternalUseCase.OrderResponse orderResponse = OrderInternalUseCase.OrderResponse.builder()
                .orderId(orderId)
                .requestId(requestId)
                .build();

        when(orderUseCase.getOrderById(orderId)).thenReturn(orderResponse);

        // When
        listener.handlePaymentSuccess(event);

        // Then
        verify(orderUseCase).markOrderAsPaid(orderId);
        verify(promotionPort).confirmCouponUsage(requestId);
    }
}
