package iuh.fit.se.modules.account.application.port.in;

import iuh.fit.se.modules.account.domain.Account;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AccountUseCase — Inbound Port (Public API).
 */
public interface AccountUseCase {

    Account getProfile(Long userId);

    Account updateProfile(Long userId, UpdateProfileCommand command);

    Account addAddress(Long userId, AddressCommand command);

    Account updateAddress(Long userId, Long addressId, AddressCommand command);

    Account deleteAddress(Long userId, Long addressId);

    void createDefaultProfile(Long userId);

    record UpdateProfileCommand(
            @NotBlank(message = "Số điện thoại không được để trống")
            String phoneNumber,
            byte[] avatarFile) {
    }

    record AddressCommand(
            @NotBlank(message = "Địa chỉ không được để trống")
            String street,
            @NotBlank(message = "Phường/Xã không được để trống")
            String ward,
            @NotBlank(message = "Quận/Huyện không được để trống")
            String district,
            @NotBlank(message = "Tỉnh/Thành phố không được để trống")
            String city,
            @NotNull(message = "Trạng thái mặc định không được để trống")
            boolean isDefault) {
    }
}
