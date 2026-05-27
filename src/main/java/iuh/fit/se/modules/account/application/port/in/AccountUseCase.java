package iuh.fit.se.modules.account.application.port.in;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * AccountUseCase - Inbound Port (Public API).
 */
public interface AccountUseCase {

    AccountProfileResponse getProfile(Long userId);

    AccountProfileResponse updateProfile(Long userId, UpdateProfileCommand command);

    AccountProfileResponse addAddress(Long userId, AddressCommand command);

    AccountProfileResponse updateAddress(Long userId, Long addressId, AddressCommand command);

    AccountProfileResponse deleteAddress(Long userId, Long addressId);

    List<ProvinceResponse> getAddressUnits();

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

    record AccountProfileResponse(
            Long id,
            Long userId,
            String phoneNumber,
            String avatarUrl,
            boolean deleted,
            List<AddressResponse> addresses) {
    }

    record AddressResponse(
            Long id,
            String recipientName,
            String phoneNumber,
            String street,
            String ward,
            String city,
            boolean isDefault) {
    }

    record ProvinceResponse(
            String code,
            String name,
            String nameEn,
            String fullName,
            String fullNameEn,
            String codeName,
            Integer administrativeUnitId,
            List<WardResponse> wards) {
    }

    record WardResponse(
            String code,
            String name,
            String nameEn,
            String fullName,
            String fullNameEn,
            String codeName,
            String provinceCode,
            Integer administrativeUnitId) {
    }
}
