package iuh.fit.se.shared.config;

import java.util.List;
import java.util.Map;

/**
 * Đối tượng Principal tùy chỉnh để lưu trữ thông tin người dùng từ JWT.
 * Sử dụng record để tự động có các phương thức accessor.
 * Giúp giải quyết lỗi SpEL trong @AuthenticationPrincipal.
 */
public record UserPrincipal(
    String email,
    Map<String, Object> claims
) {
    public Long userId() {
        if (claims == null) return null;
        Object id = claims.get("userId");
        if (id instanceof Number num) return num.longValue();
        return null;
    }

    public String role() {
        if (claims == null) return null;
        return (String) claims.get("role");
    }

    @SuppressWarnings("unchecked")
    public List<String> permissions() {
        if (claims == null) return null;
        return (List<String>) claims.get("permissions");
    }

    @Override
    public String toString() {
        return email;
    }
}
