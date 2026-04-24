package iuh.fit.se.modules.notification.application.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import iuh.fit.se.modules.notification.application.port.out.NotificationLogPersistencePort;
import iuh.fit.se.modules.notification.domain.NotificationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * NotificationSender — Component chuyên biệt xử lý việc gửi thông báo với Resilience Patterns.
 * Tách biệt khỏi NotificationService để tránh lỗi tự gọi (self-invocation) trong Spring AOP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSender {

    private final NotificationLogPersistencePort persistencePort;
    private final MeterRegistry meterRegistry;

    /**
     * Thực hiện task với cơ chế Retry (Spring Retry) và Circuit Breaker (Resilience4j).
     * CircuitBreaker bọc ngoài cùng để bảo vệ hệ thống khi lỗi diện rộng.
     */
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handleCircuitBreakerFallback")
    @Retryable(
        retryFor = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 7000, random = true)
    )
    public void sendWithResilience(NotificationLog logRecord, NotificationService.NotificationTask task) throws Exception {
        log.info("[Resilience] Attempting to send notification for event {}. Attempt: {}", 
                logRecord.getEventId(), logRecord.getAttemptCount() + 1);
        
        try {
            task.run();
            logRecord.markSuccess();
            persistencePort.save(logRecord);
            meterRegistry.counter("notification.send.success", "type", logRecord.getEventId()).increment();
        } catch (Exception e) {
            logRecord.incrementAttempt(e.getMessage());
            persistencePort.save(logRecord);
            throw e; // Để @Retryable nhận biết
        }
    }

    /**
     * @Recover cho Spring Retry — Khi hết lượt thử lại.
     */
    @Recover
    public void handleRetryExhausted(Exception e, NotificationLog logRecord, NotificationService.NotificationTask task) {
        log.error("[Resilience] FINAL FAILURE: Notification for event {} failed after all retries. Reason: {}", 
                logRecord.getEventId(), e.getMessage());
        
        logRecord.markPermanentFailure("Retry exhausted: " + e.getMessage());
        persistencePort.save(logRecord);
        
        meterRegistry.counter("notification.send.failed_permanent", "reason", "RETRY_EXHAUSTED").increment();
    }

    /**
     * Fallback cho Resilience4j Circuit Breaker — Khi mạch đang mở.
     */
    public void handleCircuitBreakerFallback(NotificationLog logRecord, NotificationService.NotificationTask task, Throwable t) {
        log.warn("[Resilience] Circuit Breaker OPEN for event {}. Failing immediately.", logRecord.getEventId());
        
        logRecord.markPermanentFailure("Circuit Breaker Open: " + t.getMessage());
        persistencePort.save(logRecord);
        
        meterRegistry.counter("notification.send.failed_permanent", "reason", "CIRCUIT_OPEN").increment();
    }
}
