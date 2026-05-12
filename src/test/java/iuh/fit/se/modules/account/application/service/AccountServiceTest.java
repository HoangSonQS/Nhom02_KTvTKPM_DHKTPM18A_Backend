package iuh.fit.se.modules.account.application.service;

import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
import iuh.fit.se.modules.account.application.port.out.AccountPersistencePort;
import iuh.fit.se.modules.account.application.port.out.ProfileImagePort;
import iuh.fit.se.modules.account.domain.Account;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.infrastructure.cloudinary.CloudinaryUploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private ProfileImagePort profileImagePort;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountPersistencePort, profileImagePort);
    }

    @Test
    void givenAccountExists_whenAddAddress_thenSuccessAndDefaultSet() {
        // Arrange
        Long userId = 1L;
        Account account = Account.createDefault(userId);
        account.setId(10L);

        when(accountPersistencePort.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountPersistencePort.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        var command = new AccountUseCase.AddressCommand("123 Street", "Ward 1", "Dist 1", "City", false);

        // Act
        Account result = accountService.addAddress(userId, command);

        // Assert
        assertThat(result.getAddresses()).hasSize(1);
        assertThat(result.getAddresses().get(0).isDefault()).isTrue(); // Tự động set default cho cái đầu tiên
        verify(accountPersistencePort).save(account);
    }

    @Test
    void givenExistingProfile_whenUpdateProfileWithImage_thenOldImageDeletedAndNewUploaded() {
        // Arrange
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
        var command = new AccountService.UpdateProfileCommand("0987654321", newImage);

        // Act
        Account result = accountService.updateProfile(userId, command);

        // Assert
        assertThat(result.getAvatarUrl()).isEqualTo("new-url");
        assertThat(result.getAvatarPublicId()).isEqualTo("new-id");
        
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
}
