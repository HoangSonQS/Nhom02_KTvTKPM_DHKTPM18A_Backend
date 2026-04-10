package iuh.fit.se.shared.audit.aspect;

import iuh.fit.se.shared.audit.annotation.Auditable;
import iuh.fit.se.shared.audit.application.port.out.AuditEventPublisherPort;
import iuh.fit.se.shared.audit.domain.event.UserActionAuditedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Aspect để tự động ghi log khi phương thức được đánh dấu @Auditable hoàn thành thành công.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditEventPublisherPort auditEventPublisherPort;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            String userId = getCurrentUserId();
            String action = auditable.action();
            
            // Đề xuất: Lấy thông tin đối tượng mục tiêu từ tham số đầu tiên nếu có
            String target = "";
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] != null) {
                target = args[0].toString();
            }

            UserActionAuditedEvent event = new UserActionAuditedEvent(
                userId,
                action,
                target,
                null, // Sẽ mở rộng lấy old/new value nếu cần phức tạp hơn
                result != null ? result.toString() : "SUCCESS",
                Instant.now()
            );

            auditEventPublisherPort.publish(event);
            
        } catch (Exception e) {
            log.error("Failed to process audit aspect", e);
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName(); // Thường là email hoặc ID
        }
        return "ANONYMOUS";
    }
}
