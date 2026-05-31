package iuh.fit.se.modules.notification.adapter.inbound.event;

import io.micrometer.core.instrument.MeterRegistry;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    @Test
    void handleOrderStatusChanged_WhenDelivered_ShouldCreateCustomerNotificationPublishRealtimeAndSendEmail() throws Exception {
        OrderStatusChangedIntegrationEvent event = new OrderStatusChangedIntegrationEvent(
                "event-delivered",
                "correlation-delivered",
                123L,
                10L,
                "Nguyen Van A",
                "customer@example.com",
                "DELIVERING",
                "DELIVERED",
                "Admin xác nhận giao hàng thành công",
                LocalDateTime.now()
        );
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<NotificationService.NotificationTask> taskCaptor =
                ArgumentCaptor.forClass(NotificationService.NotificationTask.class);
        ArgumentCaptor<RealtimeEventResponse> payloadCaptor = ArgumentCaptor.forClass(RealtimeEventResponse.class);

        listener.handleOrderStatusChanged(event);

        verify(notificationService).processCustomerNotification(
                eq("event-delivered"),
                eq(123L),
                eq(10L),
                titleCaptor.capture(),
                messageCaptor.capture(),
                eq("ORDER_STATUS_CHANGED_DELIVERED"),
                taskCaptor.capture()
        );
        verify(notificationService).publishRealtimeToUser(eq(10L), payloadCaptor.capture());
        verify(notificationService).publishRealtimeToRoles(eq(Set.of("ADMIN", "STAFF_SELLER")), eq(payloadCaptor.getValue()));

        assertEquals("ORDER_STATUS_CHANGED", payloadCaptor.getValue().type());
        assertEquals(123L, payloadCaptor.getValue().orderId());
        assertEquals(10L, payloadCaptor.getValue().userId());
        assertEquals("DELIVERED", payloadCaptor.getValue().status());
        assertEquals(titleCaptor.getValue(), payloadCaptor.getValue().message());

        taskCaptor.getValue().run();
        verify(emailPort).sendSimpleEmail(
                eq("customer@example.com"),
                eq(titleCaptor.getValue()),
                eq(messageCaptor.getValue())
        );
    }

    @Test
    void handleOrderStatusChanged_WhenNotDelivered_ShouldCreateCustomerNotificationAndSkipEmail() throws Exception {
        OrderStatusChangedIntegrationEvent event = new OrderStatusChangedIntegrationEvent(
                "event-processing",
                "correlation-processing",
                124L,
                11L,
                "Tran Thi B",
                "customer2@example.com",
                "CONFIRMED",
                "PROCESSING",
                "Admin bắt đầu xử lý đơn",
                LocalDateTime.now()
        );
        ArgumentCaptor<NotificationService.NotificationTask> taskCaptor =
                ArgumentCaptor.forClass(NotificationService.NotificationTask.class);
        ArgumentCaptor<RealtimeEventResponse> payloadCaptor = ArgumentCaptor.forClass(RealtimeEventResponse.class);

        listener.handleOrderStatusChanged(event);

        verify(notificationService).processCustomerNotification(
                eq("event-processing"),
                eq(124L),
                eq(11L),
                any(),
                any(),
                eq("ORDER_STATUS_CHANGED_PROCESSING"),
                taskCaptor.capture()
        );
        verify(notificationService).publishRealtimeToUser(eq(11L), payloadCaptor.capture());
        verify(notificationService).publishRealtimeToRoles(eq(Set.of("ADMIN", "STAFF_SELLER")), eq(payloadCaptor.getValue()));

        assertEquals("ORDER_STATUS_CHANGED", payloadCaptor.getValue().type());
        assertEquals("PROCESSING", payloadCaptor.getValue().status());

        taskCaptor.getValue().run();
        verify(emailPort, never()).sendSimpleEmail(any(), any(), any());
    }
}
