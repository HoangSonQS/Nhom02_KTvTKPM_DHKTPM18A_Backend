package iuh.fit.se.shared.audit.aspect;

import iuh.fit.se.shared.audit.annotation.Auditable;
import iuh.fit.se.shared.audit.application.port.out.AuditEventPublisherPort;
import iuh.fit.se.shared.audit.domain.event.UserActionAuditedEvent;
import iuh.fit.se.shared.config.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;

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
                getCurrentRole(),
                action,
                target,
                null, // Sẽ mở rộng lấy old/new value nếu cần phức tạp hơn
                summarizeResult(result),
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

    private String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof UserPrincipal principal && principal.role() != null) {
                return principal.role();
            }
            return authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring("ROLE_".length()))
                    .findFirst()
                    .orElse("");
        }
        return "";
    }

    private String summarizeResult(Object result) {
        if (result == null) {
            return "Thao tác thành công";
        }
        if (result instanceof Collection<?> collection) {
            return "Thao tác thành công, số bản ghi: " + collection.size();
        }
        if (result.getClass().isArray()) {
            return "Thao tác thành công, số bản ghi: " + java.lang.reflect.Array.getLength(result);
        }
        if (isSimpleValue(result)) {
            return result.toString();
        }
        return "Thao tác thành công";
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }
}
