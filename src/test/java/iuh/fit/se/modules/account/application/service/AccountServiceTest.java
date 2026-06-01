package iuh.fit.se.modules.account.application.service;

import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
import iuh.fit.se.modules.account.application.port.out.AccountPersistencePort;
import iuh.fit.se.modules.account.application.port.out.AdministrativeUnitLookupPort;
import iuh.fit.se.modules.account.application.port.out.ProfileImagePort;
import iuh.fit.se.modules.account.domain.Account;
import iuh.fit.se.modules.account.domain.AdministrativeProvince;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.event.realtime.DataChangedRealtimeEvent;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private AccountService accountService;

    @Mock
    private AccountPersistencePort accountPersistencePort;
    @Mock
    private AdministrativeUnitLookupPort administrativeUnitLookupPort;
    @Mock
    private ProfileImagePort profileImagePort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountPersistencePort, administrativeUnitLookupPort, profileImagePort, eventPublisher);
    }

    @Test
    void givenAccountExists_whenAddAddress_thenSuccessAndDefaultSet() {
        Long userId = 1L;
        Account account = Account.createDefault(userId);
        account.setId(10L);

        when(accountPersistencePort.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountPersistencePort.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        var command = new AccountUseCase.AddressCommand(
                "Test User", "0901234567", "123 Street", "Ward 1", "City", false);

        AccountUseCase.AccountProfileResponse result = accountService.addAddress(userId, command);

        assertThat(result.addresses()).hasSize(1);
        assertThat(result.addresses().get(0).isDefault()).isTrue();
        verify(accountPersistencePort).save(account);
        verify(eventPublisher).publishEvent(any(DataChangedRealtimeEvent.class));
    }

    @Test
    void givenExistingProfile_whenUpdateProfileWithImage_thenOldImageDeletedAndNewUploaded() {
        Long userId = 1L;
        Account account = Account.builder()
                .userId(userId)
                .avatarPublicId("old-id")
                .avatarUrl("old-url")
                .build();

        when(accountPersistencePort.findByUserId(userId)).thenReturn(Optional.of(account));
        when(profileImagePort.uploadAvatar(any())).thenReturn(new CloudinaryUploadResult("new-id", "new-url"));
        when(accountPersistencePort.save(any())).thenAnswer(i -> i.getArgument(0));

        byte[] newImage = "new image content".getBytes();
        var command = new AccountUseCase.UpdateProfileCommand("0987654321", newImage);

        AccountUseCase.AccountProfileResponse result = accountService.updateProfile(userId, command);

        assertThat(result.avatarUrl()).isEqualTo("new-url");
        verify(profileImagePort).deleteOldAvatar("old-id");
        verify(profileImagePort).uploadAvatar(newImage);
    }

    @Test
    void givenNoAccount_whenGetProfile_thenThrowsException() {
        when(accountPersistencePort.findByUserId(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getProfile(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void whenGetAddressUnits_thenDelegatesToLookupPort() {
        List<AdministrativeProvince> provinces = List.of(new AdministrativeProvince(
                "79", "Ho Chi Minh", null, "Thanh pho Ho Chi Minh", null, "ho_chi_minh", 1, List.of()));
        when(administrativeUnitLookupPort.findAllProvincesWithWards()).thenReturn(provinces);

        assertThat(accountService.getAddressUnits())
                .extracting(AccountUseCase.ProvinceResponse::code)
                .containsExactly("79");
        verify(administrativeUnitLookupPort).findAllProvincesWithWards();
    }
}
