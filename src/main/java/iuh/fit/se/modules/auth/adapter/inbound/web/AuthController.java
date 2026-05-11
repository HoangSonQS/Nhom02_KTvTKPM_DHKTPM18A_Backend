package iuh.fit.se.modules.auth.adapter.inbound.web;

import iuh.fit.se.modules.auth.application.port.in.AuthUseCase;
import iuh.fit.se.modules.auth.application.port.in.PasswordResetUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.config.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller cho module auth — Phase 5.5.
 * Quản lý xác thực với HttpOnly Cookie, CSRF Token và Device ID.
 * Phiên bản API: v1
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthUseCase authUseCase;
        private final PasswordResetUseCase passwordResetUseCase;

        /**
         * Bật {@code Secure} cho cookie refreshToken (chỉ gửi qua HTTPS).
         * - Dev (HTTP localhost): để false để browser chấp nhận cookie.
         * - Production (HTTPS): bắt buộc true.
         * Cấu hình qua {@code app.auth.cookie.secure}.
         */
        @Value("${app.auth.cookie.secure:false}")
        private boolean cookieSecure;

        /**
         * SameSite cho cookie refreshToken.
         * - Same-site dev (FE/BE cùng origin qua Vite proxy): {@code Lax} là đủ.
         * - Cross-site prod (FE/BE khác origin, đều HTTPS): {@code None} (yêu cầu Secure=true).
         */
        @Value("${app.auth.cookie.same-site:Lax}")
        private String cookieSameSite;

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<AuthUseCase.TokenPair>> login(
                        @Valid @RequestBody LoginRequest request,
                        @RequestHeader(value = "X-Device-ID", required = false) String deviceId,
                        HttpServletResponse response) {

                AuthUseCase.TokenPair tokens = authUseCase.login(
                                new AuthUseCase.LoginCommand(request.email(), request.password()),
                                deviceId);

                setRefreshTokenCookie(response, tokens.refreshToken());

                return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", tokens));
        }

        @PostMapping("/register")
        public ResponseEntity<ApiResponse<AuthUseCase.TokenPair>> register(
                        @Valid @RequestBody RegisterRequest request,
                        HttpServletResponse response) {

                AuthUseCase.TokenPair tokens = authUseCase.register(
                                new AuthUseCase.RegisterCommand(request.email(), request.password(),
                                                request.fullName()));

                setRefreshTokenCookie(response, tokens.refreshToken());

                return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", tokens));
        }

        @PostMapping("/refresh")
        public ResponseEntity<ApiResponse<AuthUseCase.TokenPair>> refresh(
                        @CookieValue(name = "refreshToken", required = false) String refreshToken,
                        @RequestHeader(value = "X-Device-ID", required = false) String deviceId,
                        HttpServletResponse response) {

                if (refreshToken == null || refreshToken.isBlank()) {
                        return ResponseEntity.status(401).body(ApiResponse.error(401, "Missing refresh token"));
                }

                AuthUseCase.TokenPair tokens = authUseCase.refreshToken(refreshToken, deviceId);
                setRefreshTokenCookie(response, tokens.refreshToken());

                return ResponseEntity.ok(ApiResponse.success(tokens));
        }

        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @AuthenticationPrincipal UserPrincipal principal,
                        @RequestHeader(value = "X-Device-ID", required = false) String deviceId,
                        HttpServletResponse response) {

                Long userId = principal != null ? ((Number) principal.claims().get("userId")).longValue() : null;
                authUseCase.logout(userId, deviceId);
                clearRefreshTokenCookie(response);

                return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
        }

        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
                passwordResetUseCase.requestPasswordReset(request.email());
                return ResponseEntity.ok(ApiResponse.success("Mã xác nhận đã được gửi vào email của bạn", null));
        }

        @PostMapping("/reset-password")
        public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
                passwordResetUseCase.completePasswordReset(
                                new iuh.fit.se.modules.auth.application.port.in.PasswordResetUseCase.PasswordResetCommand(
                                                request.email(), request.otp(), request.newPassword()));
                return ResponseEntity.ok(ApiResponse
                                .success("Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại.", null));
        }

        private void setRefreshTokenCookie(HttpServletResponse response, String token) {
                ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/")
                                .maxAge(7 * 24 * 3600)
                                .sameSite(cookieSameSite)
                                .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        private void clearRefreshTokenCookie(HttpServletResponse response) {
                ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                                .httpOnly(true)
                                .secure(cookieSecure)
                                .path("/")
                                .maxAge(0)
                                .sameSite(cookieSameSite)
                                .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        // ===== Request DTOs =====

        public record LoginRequest(
                        @NotBlank @Email String email,
                        @NotBlank @Size(min = 6) String password) {
        }

        public record RegisterRequest(
                        @NotBlank @Email String email,
                        @NotBlank @Size(min = 6) String password,
                        @NotBlank @Size(min = 2, max = 100) String fullName) {
        }

        public record ForgotPasswordRequest(
                        @NotBlank @Email String email) {
        }

        public record ResetPasswordRequest(
                        @NotBlank @Email String email,
                        @NotBlank String otp,
                        @NotBlank @Size(min = 6) String newPassword) {
        }
}
