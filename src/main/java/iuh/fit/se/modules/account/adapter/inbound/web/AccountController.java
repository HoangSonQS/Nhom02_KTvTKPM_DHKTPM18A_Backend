package iuh.fit.se.modules.account.adapter.inbound.web;

import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
import iuh.fit.se.modules.account.domain.Account;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountUseCase accountUseCase;

    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_SELF')")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Account>> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(accountUseCase.getProfile(userId)));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Account>> updateProfile(
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) throws IOException {

        Long userId = SecurityUtils.getCurrentUserId();
        byte[] avatarBytes = (avatar != null && !avatar.isEmpty()) ? avatar.getBytes() : null;

        Account updated = accountUseCase.updateProfile(userId,
                new AccountUseCase.UpdateProfileCommand(phoneNumber, avatarBytes));

        return ResponseEntity.ok(ApiResponse.success("Cập nhật profile thành công", updated));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @PostMapping("/address")
    public ResponseEntity<ApiResponse<Account>> addAddress(
            @Valid @RequestBody AccountUseCase.AddressCommand command) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[BE] Received addAddress request for userId: {}. Command: {}", userId, command);
        Account updated = accountUseCase.addAddress(userId, command);
        log.info("[BE] Address added successfully for userId: {}", userId);
        return ResponseEntity.ok(ApiResponse.success("Thêm địa chỉ thành công", updated));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @org.springframework.web.bind.annotation.PutMapping("/address/{id}")
    public ResponseEntity<ApiResponse<Account>> updateAddress(
            @jakarta.validation.constraints.NotNull @org.springframework.web.bind.annotation.PathVariable("id") Long addressId,
            @Valid @RequestBody AccountUseCase.AddressCommand command) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("[BE] Received updateAddress request for userId: {}, addressId: {}. Command: {}", userId, addressId, command);
        Account updated = accountUseCase.updateAddress(userId, addressId, command);
        log.info("[BE] Address updated successfully for userId: {}, addressId: {}", userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ thành công", updated));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @org.springframework.web.bind.annotation.DeleteMapping("/address/{id}")
    public ResponseEntity<ApiResponse<Account>> deleteAddress(
            @jakarta.validation.constraints.NotNull @org.springframework.web.bind.annotation.PathVariable("id") Long addressId) {

        Long userId = SecurityUtils.getCurrentUserId();
        Account updated = accountUseCase.deleteAddress(userId, addressId);

        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", updated));
    }

    /**
     * Helper: Lấy userId từ SecurityContext (được set tại JwtAuthenticationFilter).
     */
}
