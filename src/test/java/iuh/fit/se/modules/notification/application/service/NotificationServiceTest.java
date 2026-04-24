package iuh.fit.se.modules.notification.application.service;

import iuh.fit.se.modules.notification.application.port.out.NotificationLogPersistencePort;
import iuh.fit.se.modules.notification.domain.NotificationLog;
import iuh.fit.se.modules.notification.domain.NotificationStatus;
import iuh.fit.se.modules.notification.application.port.in.NotificationLogResponse;
import iuh.fit.se.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationLogPersistencePort persistencePort;

    @Mock
    private RedisRateLimiter rateLimiter;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationLog testLog;

    @BeforeEach
    void setUp() {
        testLog = NotificationLog.builder()
                .id(1L)
                .eventId("event-123")
                .orderId(123L)
                .status(NotificationStatus.FAILED_PERMANENT)
                .build();
    }

    @Test
    void getFailedNotifications_ShouldReturnMappedResponses() {
        when(persistencePort.findAll()).thenReturn(Collections.singletonList(testLog));

        List<NotificationLogResponse> result = notificationService.getFailedNotifications();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("event-123", result.get(0).getEventId());
        verify(persistencePort).findAll();
    }

    @Test
    void retryNotification_ShouldResetStatusAndSave() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(testLog));

        notificationService.retryNotification(1L);

        assertEquals(NotificationStatus.INIT, testLog.getStatus());
        verify(persistencePort).save(testLog);
    }

    @Test
    void retryNotification_WhenNotFound_ShouldThrowException() {
        when(persistencePort.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> notificationService.retryNotification(99L));
    }
}
