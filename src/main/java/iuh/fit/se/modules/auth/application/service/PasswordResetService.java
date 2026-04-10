package iuh.fit.se.modules.auth.application.service;

import iuh.fit.se.modules.auth.application.port.in.PasswordResetUseCase;
import iuh.fit.se.modules.auth.application.port.out.OtpPersistencePort;
import iuh.fit.se.modules.auth.application.port.out.RefreshTokenPersistencePort;
import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.modules.auth.domain.User;
import iuh.fit.se.shared.application.port.out.EmailPort;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService implements PasswordResetUseCase {

    private final UserPersistencePort userPersistencePort;
    private final OtpPersistencePort otpPersistencePort;
    private final EmailPort emailPort;
    private final RefreshTokenPersistencePort refreshTokenPersistencePort;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new SecureRandom();

    @Value("${jwt.otp-secret:default-secret-change-me}")
    private String otpSecret;

    @Override
    @Transactional(readOnly = true)
    public void requestPasswordReset(String email) {
        // 1. Check user existence (Security: don't reveal if email exists, but here we usually fail if not found for internal use)
        User user = userPersistencePort.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND));

        // 2. Rate Limit Check
        if (!otpPersistencePort.isAllowedToRequest(email)) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS, "Vui lòng đợi 5 phút trước khi yêu cầu mã mới.");
        }

        // 3. Generate OTP (6 digits)
        String otp = String.format("%06d", random.nextInt(1000000));
        
        // 4. Context-bound HMAC Hash
        String otpHash = calculateHmac(email, otp);

        // 5. Store Hash in Redis (5 minutes expiry)
        otpPersistencePort.saveOtpHash(email, otpHash, 300);
        otpPersistencePort.recordRequest(email);

        // 6. Send Email
        try {
            log.info("Sending Password Reset OTP to {}", email);
            emailPort.sendSimpleEmail(email, "[SEBook] Mã xác nhận đặt lại mật khẩu", 
                    "Mã xác nhận của bạn là: " + otp + ". Hiệu lực trong 5 phút.");
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể gửi email xác nhận.");
        }
    }

    @Override
    @Transactional
    public void completePasswordReset(PasswordResetCommand command) {
        // 1. Verify OTP Hash
        String storedHash = otpPersistencePort.getOtpHash(command.email())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_TOKEN_INVALID, "Mã OTP đã hết hạn hoặc không hợp lệ."));

        String inputHash = calculateHmac(command.email(), command.otp());

        if (!storedHash.equals(inputHash)) {
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID, "Mã OTP không chính xác.");
        }

        // 2. Success: Update Password
        User user = userPersistencePort.findByEmail(command.email())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND));
        
        user.updatePassword(passwordEncoder.encode(command.newPassword()));
        userPersistencePort.save(user);

        // 3. Clean up OTP (One-time use)
        otpPersistencePort.deleteOtp(command.email());

        // 4. Force Logout (Revoke all sessions)
        refreshTokenPersistencePort.revokeAllUserSessions(user.getId().toString());
        log.info("Password reset successful for user {}. All sessions revoked.", command.email());
    }

    private String calculateHmac(String email, String otp) {
        try {
            String data = email + ":" + otp;
            SecretKeySpec secretKey = new SecretKeySpec(otpSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating OTP HMAC", e);
        }
    }
}
