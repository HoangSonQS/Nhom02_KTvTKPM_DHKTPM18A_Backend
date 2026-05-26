package iuh.fit.se.modules.account.application.port.in;

import iuh.fit.se.modules.account.domain.Account;
import iuh.fit.se.modules.account.domain.AdministrativeProvince;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * AccountUseCase - Inbound Port (Public API).
 */
public interface AccountUseCase {

    Account getProfile(Long userId);

    Account updateProfile(Long userId, UpdateProfileCommand command);

    Account addAddress(Long userId, AddressCommand command);

    Account updateAddress(Long userId, Long addressId, AddressCommand command);

    Account deleteAddress(Long userId, Long addressId);

    List<AdministrativeProvince> getAddressUnits();

    void createDefaultProfile(Long userId);

    record UpdateProfileCommand(
            @NotBlank(message = "So dien thoai khong duoc de trong")
            String phoneNumber,
            byte[] avatarFile) {
    }

    record AddressCommand(
            @NotBlank(message = "Ten nguoi nhan khong duoc de trong")
            String recipientName,
            @NotBlank(message = "So dien thoai khong duoc de trong")
            String phoneNumber,
            @NotBlank(message = "Dia chi khong duoc de trong")
            String street,
            @NotBlank(message = "Phuong/Xa khong duoc de trong")
            String ward,
            @NotBlank(message = "Tinh/Thanh pho khong duoc de trong")
            String city,
            boolean isDefault) {
    }
}
