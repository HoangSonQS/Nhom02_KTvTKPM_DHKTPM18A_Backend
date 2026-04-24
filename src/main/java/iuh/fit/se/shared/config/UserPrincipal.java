package iuh.fit.se.shared.config;

import java.util.Map;

/**
 * Đối tượng Principal tùy chỉnh để lưu trữ thông tin người dùng từ JWT.
 * Sử dụng record để tự động có các phương thức accessor (email(), claims()).
 * Giúp giải quyết lỗi SpEL trong @AuthenticationPrincipal.
 */
public record UserPrincipal(
    String email,
    Map<String, Object> claims
) {
    @Override
    public String toString() {
        return email;
    }
}
