package iuh.fit.se.modules.auth.adapter.inbound.web;

import iuh.fit.se.modules.auth.application.port.in.AuthUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller cho module auth.
 * Chỉ được phép: nhận HTTP request, gọi AuthUseCase, trả ApiResponse.
 * Không được chứa business logic. Không được inject Repository.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthUseCase.TokenPair>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthUseCase.TokenPair tokens = authUseCase.login(
                new AuthUseCase.LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", tokens));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthUseCase.TokenPair>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthUseCase.TokenPair tokens = authUseCase.register(
                new AuthUseCase.RegisterCommand(request.email(), request.password(), request.fullName()));
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthUseCase.TokenPair>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        AuthUseCase.TokenPair tokens = authUseCase.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    // ===== Request DTOs =====

    record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password) {}

    record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password,
            @NotBlank @Size(min = 2, max = 100) String fullName) {}

    record RefreshRequest(@NotBlank String refreshToken) {}
}
