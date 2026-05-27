package iuh.fit.se.modules.account.application.service;

import iuh.fit.se.modules.account.application.port.in.AccountInternalUseCase;
import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
import iuh.fit.se.modules.account.application.port.out.AccountPersistencePort;
import iuh.fit.se.modules.account.application.port.out.AdministrativeUnitLookupPort;
import iuh.fit.se.modules.account.application.port.out.ProfileImagePort;
import iuh.fit.se.modules.account.domain.Account;
import iuh.fit.se.modules.account.domain.Address;
import iuh.fit.se.modules.account.domain.AdministrativeProvince;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService implements AccountUseCase, AccountInternalUseCase {

    private final AccountPersistencePort accountPersistencePort;
    private final AdministrativeUnitLookupPort administrativeUnitLookupPort;
    private final ProfileImagePort profileImagePort;

    @Override
    @Transactional(readOnly = true)
    public AccountProfileResponse getProfile(Long userId) {
        return mapToProfileResponse(findAccountByUserId(userId));
    }

    @Override
    @Transactional
    public AccountProfileResponse updateProfile(Long userId, UpdateProfileCommand command) {
        Account account = findAccountByUserId(userId);

        String avatarUrl = account.getAvatarUrl();
        String avatarPublicId = account.getAvatarPublicId();

        if (command.avatarFile() != null && command.avatarFile().length > 0) {
            if (avatarPublicId != null) {
                profileImagePort.deleteOldAvatar(avatarPublicId);
            }
            CloudinaryUploadResult result = profileImagePort.uploadAvatar(command.avatarFile());
            avatarUrl = result.url();
            avatarPublicId = result.publicId();
        }

        account.updateProfile(command.phoneNumber(), avatarUrl, avatarPublicId);
        return mapToProfileResponse(accountPersistencePort.save(account));
    }

    @Override
    @Transactional
    public AccountProfileResponse addAddress(Long userId, AddressCommand command) {
        Account account = findAccountByUserId(userId);

        Address address = Address.builder()
                .recipientName(command.recipientName())
                .phoneNumber(command.phoneNumber())
                .street(command.street())
                .ward(command.ward())
                .city(command.city())
                .isDefault(command.isDefault())
                .build();

        account.addAddress(address);
        return mapToProfileResponse(accountPersistencePort.save(account));
    }

    @Override
    @Transactional
    public AccountProfileResponse updateAddress(Long userId, Long addressId, AddressCommand command) {
        Account account = findAccountByUserId(userId);

        Address updatedData = Address.builder()
                .recipientName(command.recipientName())
                .phoneNumber(command.phoneNumber())
                .street(command.street())
                .ward(command.ward())
                .city(command.city())
                .isDefault(command.isDefault())
                .build();

        account.updateAddress(addressId, updatedData);
        return mapToProfileResponse(accountPersistencePort.save(account));
    }

    @Override
    @Transactional
    public AccountProfileResponse deleteAddress(Long userId, Long addressId) {
        Account account = findAccountByUserId(userId);
        account.removeAddress(addressId);
        return mapToProfileResponse(accountPersistencePort.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvinceResponse> getAddressUnits() {
        return administrativeUnitLookupPort.findAllProvincesWithWards().stream()
                .map(this::mapToProvinceResponse)
                .toList();
    }

    @Override
    @Transactional
    public void createDefaultProfile(Long userId) {
        if (accountPersistencePort.findByUserId(userId).isPresent()) {
            return;
        }

        Account account = Account.createDefault(userId);
        accountPersistencePort.save(account);
    }

    private Account findAccountByUserId(Long userId) {
        return accountPersistencePort.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong tim thay profile"));
    }

    private AccountProfileResponse mapToProfileResponse(Account account) {
        return new AccountProfileResponse(
                account.getId(),
                account.getUserId(),
                account.getPhoneNumber(),
                account.getAvatarUrl(),
                account.isDeleted(),
                account.getAddresses().stream()
                        .map(address -> new AddressResponse(
                                address.getId(),
                                address.getRecipientName(),
                                address.getPhoneNumber(),
                                address.getStreet(),
                                address.getWard(),
                                address.getCity(),
                                address.isDefault()))
                        .toList());
    }

    private ProvinceResponse mapToProvinceResponse(AdministrativeProvince province) {
        return new ProvinceResponse(
                province.code(),
                province.name(),
                province.nameEn(),
                province.fullName(),
                province.fullNameEn(),
                province.codeName(),
                province.administrativeUnitId(),
                province.wards().stream()
                        .map(ward -> new WardResponse(
                                ward.code(),
                                ward.name(),
                                ward.nameEn(),
                                ward.fullName(),
                                ward.fullNameEn(),
                                ward.codeName(),
                                ward.provinceCode(),
                                ward.administrativeUnitId()))
                        .toList());
    }
}
