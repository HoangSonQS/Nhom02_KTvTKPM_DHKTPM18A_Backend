package iuh.fit.se.modules.account.adapter.inbound.web;

import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
import iuh.fit.se.modules.account.domain.Account;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * AccountController — Inbound Adapter cho Account.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountUseCase accountUseCase;

    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_SELF')")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Account>> getProfile() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(accountUseCase.getProfile(userId)));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Account>> updateProfile(
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) throws IOException {

        Long userId = getCurrentUserId();
        byte[] avatarBytes = (avatar != null && !avatar.isEmpty()) ? avatar.getBytes() : null;

        Account updated = accountUseCase.updateProfile(userId,
                new AccountUseCase.UpdateProfileCommand(phoneNumber, avatarBytes));

        return ResponseEntity.ok(ApiResponse.success("Cập nhật profile thành công", updated));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @PostMapping("/address")
    public ResponseEntity<ApiResponse<Account>> addAddress(
            @Valid @RequestBody AccountUseCase.AddressCommand command) {

        Long userId = getCurrentUserId();
        Account updated = accountUseCase.addAddress(userId, command);

        return ResponseEntity.ok(ApiResponse.success("Thêm địa chỉ thành công", updated));
    }

    /**
     * Helper: Lấy userId từ SecurityContext (được set tại JwtAuthenticationFilter).
     */
    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Không tìm thấy thông tin xác thực");
        }
        return (Long) auth.getCredentials();
    }
}
