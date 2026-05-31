package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.realtime.OrderRealtimeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderRealtimeEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderRealtimeEventListener listener;

    @Test
    void handleOrderRealtime_WhenOrderStatusChanges_ShouldPublishToAdminSellerAndCustomer() {
        OrderRealtimeEvent event = OrderRealtimeEvent.statusChanged(
                12L,
                5L,
                new BigDecimal("100000"),
                "CONFIRMED"
        );
        ArgumentCaptor<RealtimeEventResponse> payloadCaptor =
                ArgumentCaptor.forClass(RealtimeEventResponse.class);

        listener.handleOrderRealtime(event);

        verify(notificationService).publishRealtimeToRoles(
                eq(Set.of("ADMIN", "STAFF_SELLER")),
                payloadCaptor.capture()
        );
        verify(notificationService).publishRealtimeToUser(eq(5L), eq(payloadCaptor.getValue()));
        assertEquals("ORDER_STATUS_CHANGED", payloadCaptor.getValue().type());
        assertEquals(12L, payloadCaptor.getValue().orderId());
        assertEquals("CONFIRMED", payloadCaptor.getValue().status());
    }
}
