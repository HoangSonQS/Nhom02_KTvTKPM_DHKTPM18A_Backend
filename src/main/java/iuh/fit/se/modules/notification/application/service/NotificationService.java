package iuh.fit.se.modules.notification.application.service;

import iuh.fit.se.modules.notification.adapter.outbound.persistence.NotificationLogRepository;
import iuh.fit.se.modules.notification.application.port.in.NotificationAdminPort;
import iuh.fit.se.modules.notification.application.port.in.NotificationLogResponse;
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
import java.util.stream.Collectors;

/**
 * NotificationService — Implement các logic nghiệp vụ về thông báo và quản trị log.
 * Tích hợp Resilience Patterns: Retry (Spring Retry) + Circuit Breaker (Resilience4j).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements NotificationAdminPort {

    private final NotificationLogRepository logRepository;
    private final RedisRateLimiter rateLimiter;
    private final NotificationSender notificationSender;

    // --- Admin Operations (NotificationAdminPort) ---

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLogResponse> getFailedNotifications() {
        return logRepository.findAll().stream()
                .filter(l -> l.getStatus() == NotificationStatus.FAILED_PERMANENT)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void retryNotification(Long logId) {
        NotificationLog nLog = logRepository.findById(logId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT, "Không tìm thấy log thông báo"));

        log.info("Admin triggered manual retry for Notification Log ID: {}, Event: {}", logId, nLog.getEventId());
        
        // Fix (Audit Design): Reset trạng thái INIT thay vì xóa log để giữ audit history
        nLog.resetToInit();
        logRepository.save(nLog);
    }

    // --- Internal Business Logic ---

    @Transactional
    public void processNotification(String eventId, Long orderId, String type, NotificationTask task) {
        // 1. Idempotency check (DB-level)
        NotificationLog notificationLog;
        try {
            notificationLog = logRepository.saveAndFlush(NotificationLog.builder()
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
            logRepository.save(notificationLog);
            return;
        }

        // 3. Thực thi với Resilience Stack (thông qua NotificationSender Bean để tránh self-invocation)
        try {
            notificationSender.sendWithResilience(notificationLog, task);
        } catch (Exception e) {
            log.error("Notification process failed for event {}: {}", eventId, e.getMessage());
        }
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

    @FunctionalInterface
    public interface NotificationTask {
        void run() throws Exception;
    }
}
