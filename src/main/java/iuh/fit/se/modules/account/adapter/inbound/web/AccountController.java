package iuh.fit.se.modules.account.adapter.inbound.web;

import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
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
import java.util.List;

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
    public ResponseEntity<ApiResponse<AccountUseCase.AccountProfileResponse>> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(accountUseCase.getProfile(userId)));
    }

    @GetMapping("/address-units")
    public ResponseEntity<ApiResponse<List<AccountUseCase.ProvinceResponse>>> getAddressUnits() {
        return ResponseEntity.ok(ApiResponse.success(accountUseCase.getAddressUnits()));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<AccountUseCase.AccountProfileResponse>> updateProfile(
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) throws IOException {

        Long userId = SecurityUtils.getCurrentUserId();
        byte[] avatarBytes = (avatar != null && !avatar.isEmpty()) ? avatar.getBytes() : null;

        AccountUseCase.AccountProfileResponse updated = accountUseCase.updateProfile(userId,
                new AccountUseCase.UpdateProfileCommand(phoneNumber, avatarBytes));

        return ResponseEntity.ok(ApiResponse.success("Cập nhật profile thành công", updated));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @PostMapping("/address")
    public ResponseEntity<ApiResponse<AccountUseCase.AccountProfileResponse>> addAddress(
            @Valid @RequestBody AddressRequestBody body) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.debug("[BE] addAddress userId={}", userId);
        AccountUseCase.AccountProfileResponse updated = accountUseCase.addAddress(userId, body.toCommand());
        return ResponseEntity.ok(ApiResponse.success("Thêm địa chỉ thành công", updated));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @org.springframework.web.bind.annotation.PutMapping("/address/{id}")
    public ResponseEntity<ApiResponse<AccountUseCase.AccountProfileResponse>> updateAddress(
            @jakarta.validation.constraints.NotNull @org.springframework.web.bind.annotation.PathVariable("id") Long addressId,
            @Valid @RequestBody AddressRequestBody body) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.debug("[BE] updateAddress userId={} addressId={}", userId, addressId);
        AccountUseCase.AccountProfileResponse updated = accountUseCase.updateAddress(userId, addressId, body.toCommand());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ thành công", updated));
    }

    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_SELF')")
    @org.springframework.web.bind.annotation.DeleteMapping("/address/{id}")
    public ResponseEntity<ApiResponse<AccountUseCase.AccountProfileResponse>> deleteAddress(
            @jakarta.validation.constraints.NotNull @org.springframework.web.bind.annotation.PathVariable("id") Long addressId) {

        Long userId = SecurityUtils.getCurrentUserId();
        AccountUseCase.AccountProfileResponse updated = accountUseCase.deleteAddress(userId, addressId);

        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", updated));
    }

    /**
     * Helper: Lấy userId từ SecurityContext (được set tại JwtAuthenticationFilter).
     */
}
