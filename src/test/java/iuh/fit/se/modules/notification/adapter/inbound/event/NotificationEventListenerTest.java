package iuh.fit.se.modules.notification.adapter.inbound.event;

import io.micrometer.core.instrument.MeterRegistry;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.application.port.out.EmailPort;
import iuh.fit.se.shared.event.order.OrderStatusChangedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailPort emailPort;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void handleOrderStatusChanged_ShouldUseStatusSpecificRateLimitType() {
        OrderStatusChangedIntegrationEvent event = new OrderStatusChangedIntegrationEvent(
                "event-1",
                "correlation-1",
                123L,
                10L,
                "Nguyen Van A",
                "customer@example.com",
                "DELIVERING",
                "DELIVERED",
                null,
                LocalDateTime.now()
        );
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);

        listener.handleOrderStatusChanged(event);

        verify(notificationService).processCustomerNotification(
                eq("event-1"),
                eq(123L),
                eq(10L),
                any(),
                any(),
                typeCaptor.capture(),
                any()
        );
        assertEquals("ORDER_STATUS_CHANGED_DELIVERED", typeCaptor.getValue());
    }
}
