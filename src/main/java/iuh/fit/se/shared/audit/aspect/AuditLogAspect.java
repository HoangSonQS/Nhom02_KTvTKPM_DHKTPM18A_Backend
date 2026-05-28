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
            
            Object[] args = joinPoint.getArgs();
            String target = describeTarget(action, args, result);

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

    private String describeTarget(String action, Object[] args, Object result) {
        return switch (action) {
            case "ADMIN_LOCK_USER" -> "ID tài khoản " + firstArg(args);
            case "STAFF_INIT_STOCK", "STAFF_INCREASE_STOCK", "STAFF_DECREASE_STOCK" -> "ID sách " + firstArg(args);
            case "STAFF_UPDATE_BOOK" -> "ID sách " + firstArg(args);
            case "STAFF_DELETE_CATEGORY", "STAFF_UPDATE_CATEGORY" -> "ID danh mục " + firstArg(args);
            case "STAFF_APPROVE_RETURN", "STAFF_RECEIVE_RETURN", "STAFF_REFUND_RETURN", "STAFF_REJECT_RETURN" ->
                    "ID yêu cầu trả hàng " + firstArg(args);
            case "ADMIN_CREATE_STAFF" -> "Tài khoản nhân viên " + safeResultIdentity(result);
            case "STAFF_CREATE_BOOK" -> "Sách " + safeResultIdentity(result);
            case "STAFF_CREATE_CATEGORY" -> "Danh mục " + safeResultIdentity(result);
            default -> firstSimpleArg(args);
        };
    }

    private String firstArg(Object[] args) {
        return args.length > 0 && args[0] != null ? String.valueOf(args[0]) : "-";
    }

    private String firstSimpleArg(Object[] args) {
        if (args.length == 0 || args[0] == null) {
            return "";
        }
        Object value = args[0];
        return isSimpleValue(value) ? value.toString() : "Thao tác hệ thống";
    }

    private String safeResultIdentity(Object result) {
        if (result == null) {
            return "mới";
        }
        String id = readAccessor(result, "id");
        String name = firstNonBlank(
                readAccessor(result, "fullName"),
                readAccessor(result, "name"),
                readAccessor(result, "title"),
                readAccessor(result, "email")
        );
        if (name != null && id != null) {
            return name + " (ID " + id + ")";
        }
        if (name != null) {
            return name;
        }
        if (id != null) {
            return "ID " + id;
        }
        return "mới";
    }

    private String readAccessor(Object source, String accessorName) {
        try {
            Object value = source.getClass().getMethod(accessorName).invoke(source);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ignored) {
            try {
                String getter = "get" + Character.toUpperCase(accessorName.charAt(0)) + accessorName.substring(1);
                Object value = source.getClass().getMethod(getter).invoke(source);
                return value == null ? null : value.toString();
            } catch (ReflectiveOperationException ignoredGetter) {
                return null;
            }
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }
}
