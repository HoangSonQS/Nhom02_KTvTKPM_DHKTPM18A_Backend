package iuh.fit.se.modules.auth.application.port.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Port/In cho luồng Quên mật khẩu.
 */
public interface PasswordResetUseCase {
    
    /**
     * Yêu cầu gửi OTP tới email.
     */
    void requestPasswordReset(@NotBlank @Email String email);

    /**
     * Xác nhận OTP và đổi mật khẩu mới.
     */
    void completePasswordReset(PasswordResetCommand command);

    record PasswordResetCommand(
        @NotBlank @Email String email,
        @NotBlank String otp,
        @NotBlank @Size(min = 6) String newPassword
    ) {}
}
