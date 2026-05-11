package iuh.fit.se.shared.security;

import iuh.fit.se.shared.config.UserPrincipal;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Không tìm thấy thông tin xác thực");
        }
        return principal;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().userId();
    }

    public static String getCurrentEmail() {
        return getCurrentUser().email();
    }

    public static String getCurrentRole() {
        return getCurrentUser().role();
    }
}
