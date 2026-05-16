package iuh.fit.se.modules.account.application.service;

import iuh.fit.se.modules.account.application.port.in.AccountInternalUseCase;
import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
import iuh.fit.se.modules.account.application.port.out.AccountPersistencePort;
import iuh.fit.se.modules.account.application.port.out.AdministrativeUnitLookupPort;
import iuh.fit.se.modules.account.application.port.out.ProfileImagePort;
import iuh.fit.se.modules.account.domain.Account;
import iuh.fit.se.modules.account.domain.AdministrativeProvince;
import iuh.fit.se.modules.account.domain.Address;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AccountService — Implementation của các UseCase liên quan đến Account.
 */
@Service
@RequiredArgsConstructor
public class AccountService implements AccountUseCase, AccountInternalUseCase {

    private final AccountPersistencePort accountPersistencePort;
    private final AdministrativeUnitLookupPort administrativeUnitLookupPort;
    private final ProfileImagePort profileImagePort;

    @Override
    @Transactional(readOnly = true)
    public Account getProfile(Long userId) {
        return accountPersistencePort.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy profile"));
    }

    @Override
    @Transactional
    public Account updateProfile(Long userId, UpdateProfileCommand command) {
        Account account = getProfile(userId);

        String avatarUrl = account.getAvatarUrl();
        String avatarPublicId = account.getAvatarPublicId();

        // Xử lý upload ảnh nếu có
        if (command.avatarFile() != null && command.avatarFile().length > 0) {
            // Xóa ảnh cũ
            if (avatarPublicId != null) {
                profileImagePort.deleteOldAvatar(avatarPublicId);
            }
            // Upload mới
            CloudinaryUploadResult result = profileImagePort.uploadAvatar(command.avatarFile());
            avatarUrl = result.url();
            avatarPublicId = result.publicId();
        }

        account.updateProfile(command.phoneNumber(), avatarUrl, avatarPublicId);
        return accountPersistencePort.save(account);
    }

    @Override
    @Transactional
    public Account addAddress(Long userId, AddressCommand command) {
        Account account = getProfile(userId);

        Address address = Address.builder()
                .recipientName(command.recipientName())
                .phoneNumber(command.phoneNumber())
                .street(command.street())
                .ward(command.ward())
                .city(command.city())
                .isDefault(command.isDefault())
                .build();

        account.addAddress(address);
        return accountPersistencePort.save(account);
    }

    @Override
    @Transactional
    public Account updateAddress(Long userId, Long addressId, AddressCommand command) {
        Account account = getProfile(userId);

        Address updatedData = Address.builder()
                .recipientName(command.recipientName())
                .phoneNumber(command.phoneNumber())
                .street(command.street())
                .ward(command.ward())
                .city(command.city())
                .isDefault(command.isDefault())
                .build();

        account.updateAddress(addressId, updatedData);
        return accountPersistencePort.save(account);
    }

    @Override
    @Transactional
    public Account deleteAddress(Long userId, Long addressId) {
        Account account = getProfile(userId);
        account.removeAddress(addressId);
        return accountPersistencePort.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdministrativeProvince> getAddressUnits() {
        return administrativeUnitLookupPort.findAllProvincesWithWards();
    }

    @Override
    @Transactional
    public void createDefaultProfile(Long userId) {
        // Tránh tạo trùng lặp
        if (accountPersistencePort.findByUserId(userId).isPresent()) {
            return;
        }

        Account account = Account.createDefault(userId);
        accountPersistencePort.save(account);
    }
}
