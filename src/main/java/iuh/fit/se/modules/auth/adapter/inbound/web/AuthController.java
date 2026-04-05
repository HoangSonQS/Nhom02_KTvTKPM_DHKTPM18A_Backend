package iuh.fit.se.modules.auth.adapter.inbound.web;

import iuh.fit.se.modules.auth.application.port.in.AuthUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthUseCase.TokenPair>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Device-ID", required = false) String deviceId,
            HttpServletResponse response) {

        AuthUseCase.TokenPair tokens = authUseCase.login(
                new AuthUseCase.LoginCommand(request.email(), request.password()), 
                deviceId
        );

        setRefreshTokenCookie(response, tokens.refreshToken());

        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", tokens));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthUseCase.TokenPair>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthUseCase.TokenPair tokens = authUseCase.register(
                new AuthUseCase.RegisterCommand(request.email(), request.password(), request.fullName())
        );

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
            @AuthenticationPrincipal(expression = "claims['userId']") Long userId,
            @RequestHeader(value = "X-Device-ID", required = false) String deviceId,
            HttpServletResponse response) {

        authUseCase.logout(userId, deviceId);
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(true) // Bật lên trong production (HTTPS)
                .path("/")
                .maxAge(7 * 24 * 3600)
                .sameSite("Lax") // UX ổn định, chặn CSRF cho POST
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
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
}
