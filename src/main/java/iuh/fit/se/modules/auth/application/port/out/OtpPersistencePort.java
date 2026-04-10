package iuh.fit.se.modules.auth.application.port.out;

import java.util.Optional;

/**
 * Port để lưu trữ và xác thực OTP (One-Time Password).
 */
public interface OtpPersistencePort {
    /**
     * Lưu hash của OTP kèm TTL.
     */
    void saveOtpHash(String email, String otpHash, long expirySeconds);

    /**
     * Lấy hash của OTP hiện tại.
     */
    Optional<String> getOtpHash(String email);

    /**
     * Xóa OTP (One-time use).
     */
    void deleteOtp(String email);
    
    /**
     * Kiểm tra Rate Limit cho việc gửi OTP.
     */
    boolean isAllowedToRequest(String email);
    
    /**
     * Ghi nhận một lần yêu cầu gửi OTP thành công.
     */
    void recordRequest(String email);
}
