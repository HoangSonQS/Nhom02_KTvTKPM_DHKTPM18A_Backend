package iuh.fit.se.modules.notification.application.service;

import iuh.fit.se.modules.notification.application.port.in.NotificationAdminPort;
import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;
import iuh.fit.se.modules.notification.application.port.in.NotificationLogResponse;
import iuh.fit.se.modules.notification.application.port.in.NotificationCustomerUseCase;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.port.out.NotificationLogPersistencePort;
import iuh.fit.se.modules.notification.application.port.out.NotificationRealtimePort;
import iuh.fit.se.modules.notification.domain.NotificationLog;
import iuh.fit.se.modules.notification.domain.NotificationStatus;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * NotificationService — Implement các logic nghiệp vụ về thông báo và quản trị log.
 * Tích hợp Resilience Patterns: Retry (Spring Retry) + Circuit Breaker (Resilience4j).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements NotificationAdminPort, NotificationCustomerUseCase {

    private final NotificationLogPersistencePort persistencePort;
    private final RedisRateLimiter rateLimiter;
    private final NotificationSender notificationSender;
    private final NotificationRealtimePort realtimePort;

    // --- Admin Operations (NotificationAdminPort) ---

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLogResponse> getFailedNotifications() {
        return persistencePort.findAll().stream()
                .filter(l -> l.getStatus() == NotificationStatus.FAILED_PERMANENT)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void retryNotification(Long logId) {
        NotificationLog nLog = persistencePort.findById(logId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "Không tìm thấy log thông báo"));

        log.info("Admin triggered manual retry for Notification Log ID: {}, Event: {}", logId, nLog.getEventId());
        
        // Fix (Audit Design): Reset trạng thái INIT thay vì xóa log để giữ audit history
        nLog.resetToInit();
        persistencePort.save(nLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerNotificationResponse> getMyNotifications(Long userId) {
        return persistencePort.findByRecipientUserId(userId).stream()
                .map(this::mapToCustomerResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return persistencePort.countUnreadByRecipientUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        NotificationLog notification = persistencePort.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "KhÃ´ng tÃ¬m tháº¥y thÃ´ng bÃ¡o"));
        if (!userId.equals(notification.getRecipientUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "KhÃ´ng cÃ³ quyá»n Ä‘á»c thÃ´ng bÃ¡o nÃ y");
        }
        notification.markRead();
        persistencePort.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        persistencePort.findByRecipientUserId(userId).forEach(notification -> {
            notification.markRead();
            persistencePort.save(notification);
        });
    }

    // --- Internal Business Logic ---

    @Transactional
    public void processNotification(String eventId, Long orderId, String type, NotificationTask task) {
        // 1. Idempotency check (DB-level)
        NotificationLog notificationLog;
        try {
            notificationLog = persistencePort.save(NotificationLog.builder()
                    .eventId(eventId)
                    .orderId(orderId)
                    .status(NotificationStatus.INIT)
                    .channel("EMAIL")
                    .attemptCount(0)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Event {} already processed or being processed. Skipping.", eventId);
            return;
        }

        // 2. Rate limiting check (Redis)
        if (!rateLimiter.allowRequest(orderId, type)) {
            notificationLog.markPermanentFailure("Rate limit triggered (Principal Standard)");
            persistencePort.save(notificationLog);
            return;
        }

        // 3. Thực thi với Resilience Stack (thông qua NotificationSender Bean để tránh self-invocation)
        try {
            notificationSender.sendWithResilience(notificationLog, task);
        } catch (Exception e) {
            log.error("Notification process failed for event {}: {}", eventId, e.getMessage());
        }
    }

    @Transactional
    public void processCustomerNotification(
            String eventId,
            Long orderId,
            Long recipientUserId,
            String title,
            String message,
            String type,
            NotificationTask task
    ) {
        NotificationLog notificationLog;
        try {
            notificationLog = persistencePort.save(NotificationLog.builder()
                    .eventId(eventId)
                    .orderId(orderId)
                    .recipientUserId(recipientUserId)
                    .title(title)
                    .message(message)
                    .status(NotificationStatus.INIT)
                    .channel("EMAIL_WEB")
                    .attemptCount(0)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Event {} already processed or being processed. Skipping.", eventId);
            return;
        }

        realtimePort.publish(recipientUserId, mapToCustomerResponse(notificationLog));

        if (!rateLimiter.allowRequest(orderId, type)) {
            notificationLog.markPermanentFailure("Rate limit triggered (Principal Standard)");
            persistencePort.save(notificationLog);
            return;
        }

        try {
            notificationSender.sendWithResilience(notificationLog, task);
        } catch (Exception e) {
            log.error("Customer notification process failed for event {}: {}", eventId, e.getMessage());
        }
    }

    public void publishRealtimeToUser(Long userId, RealtimeEventResponse event) {
        if (userId == null) {
            return;
        }
        realtimePort.publishEventToUser(userId, event);
    }

    public void publishRealtimeToRoles(Set<String> roles, RealtimeEventResponse event) {
        realtimePort.publishEventToRoles(roles, event);
    }

    private NotificationLogResponse mapToResponse(NotificationLog log) {
        return NotificationLogResponse.builder()
                .id(log.getId())
                .eventId(log.getEventId())
                .orderId(log.getOrderId())
                .status(log.getStatus())
                .channel(log.getChannel())
                .attemptCount(log.getAttemptCount())
                .lastError(log.getLastError())
                .createdAt(log.getCreatedAt())
                .processedAt(log.getProcessedAt())
                .build();
    }

    private CustomerNotificationResponse mapToCustomerResponse(NotificationLog log) {
        return CustomerNotificationResponse.builder()
                .id(log.getId())
                .orderId(log.getOrderId())
                .title(log.getTitle())
                .message(log.getMessage())
                .channel(log.getChannel())
                .createdAt(log.getCreatedAt())
                .readAt(log.getReadAt())
                .build();
    }

    @FunctionalInterface
    public interface NotificationTask {
        void run() throws Exception;
    }
}
