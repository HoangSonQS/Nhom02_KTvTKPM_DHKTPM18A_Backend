package iuh.fit.se.modules.auth.application.port.in;

/**
 * PUBLIC Inbound Port — giao diện mà Controller (và module khác nếu cần) gọi
 * vào module auth.
 * Đây là "hợp đồng công khai" của module auth với thế giới bên ngoài.
 * Mọi thay đổi ở đây ảnh hưởng đến caller — thay đổi cẩn thận.
 */
public interface AuthUseCase {

    /**
     * Đăng nhập bằng email + password. Server sẽ cấp deviceId nếu chưa có.
     * 
     * @return TokenPair chứa accessToken, refreshToken và deviceId
     */
    TokenPair login(LoginCommand command, String deviceId);

    /**
     * Đăng ký tài khoản mới (role mặc định: CUSTOMER).
     * 
     * @return TokenPair ngay sau khi đăng ký thành công
     */
    TokenPair register(RegisterCommand command);

    /**
     * Làm mới access token bằng refresh token. Kiểm tra rv version trong Redis.
     */
    TokenPair refreshToken(String refreshToken, String deviceId);

    /**
     * Đăng xuất: Vô hiệu hóa session của thiết bị hiện tại trong Redis.
     */
    void logout(Long userId, String deviceId);

    // ===== Nested Command / Result types =====

    record LoginCommand(String email, String password) {
    }

    record RegisterCommand(String email, String password, String fullName) {
    }

    record TokenPair(String accessToken, String refreshToken, String deviceId) {
    }
}
