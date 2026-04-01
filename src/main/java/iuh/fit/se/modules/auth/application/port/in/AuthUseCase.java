package iuh.fit.se.modules.auth.application.port.in;

/**
 * PUBLIC Inbound Port — giao diện mà Controller (và module khác nếu cần) gọi vào module auth.
 * Đây là "hợp đồng công khai" của module auth với thế giới bên ngoài.
 * Mọi thay đổi ở đây ảnh hưởng đến caller — thay đổi cẩn thận.
 */
public interface AuthUseCase {

    /**
     * Đăng nhập bằng email + password.
     * @return TokenPair chứa accessToken và refreshToken
     */
    TokenPair login(LoginCommand command);

    /**
     * Đăng ký tài khoản mới (role mặc định: CUSTOMER).
     * @return TokenPair ngay sau khi đăng ký thành công
     */
    TokenPair register(RegisterCommand command);

    /**
     * Làm mới access token bằng refresh token còn hạn.
     */
    TokenPair refreshToken(String refreshToken);

    // ===== Nested Command / Result types =====

    record LoginCommand(String email, String password) {}

    record RegisterCommand(String email, String password, String fullName) {}

    record TokenPair(String accessToken, String refreshToken) {}
}
