package iuh.fit.se.modules.auth.application.service;

import iuh.fit.se.modules.auth.application.port.in.AdminUserUseCase;
import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.modules.auth.domain.Role;
import iuh.fit.se.modules.auth.domain.User;
import iuh.fit.se.modules.account.application.port.in.AccountInternalUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    @Test
    void givenUsers_whenListUsers_thenReturnSafeSummariesWithoutPassword() {
        UserPersistencePort persistencePort = mock(UserPersistencePort.class);
        AccountInternalUseCase accountInternalUseCase = mock(AccountInternalUseCase.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(persistencePort.findAll()).thenReturn(List.of(
                User.builder()
                        .id(1L)
                        .email("admin@sebook.local")
                        .password("secret")
                        .fullName("Admin SEBook")
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build()
        ));

        AdminUserService service = new AdminUserService(persistencePort, accountInternalUseCase, passwordEncoder);

        List<AdminUserUseCase.UserSummary> users = service.listUsers();

        assertThat(users).containsExactly(new AdminUserUseCase.UserSummary(
                1L,
                "admin@sebook.local",
                "Admin SEBook",
                "ADMIN",
                true
        ));
    }

    @Test
    void givenEnabledUser_whenLockUser_thenPersistDisabledUser() {
        UserPersistencePort persistencePort = mock(UserPersistencePort.class);
        AccountInternalUseCase accountInternalUseCase = mock(AccountInternalUseCase.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User user = User.builder()
                .id(2L)
                .email("customer@sebook.local")
                .password("secret")
                .fullName("Customer")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        when(persistencePort.findById(2L)).thenReturn(java.util.Optional.of(user));
        when(persistencePort.save(user)).thenReturn(user);

        AdminUserService service = new AdminUserService(persistencePort, accountInternalUseCase, passwordEncoder);

        AdminUserUseCase.UserSummary summary = service.lockUser(2L);

        assertThat(summary.enabled()).isFalse();
        verify(persistencePort).save(user);
    }

    @Test
    void givenValidStaffCommand_whenCreateStaff_thenEncodePasswordAndCreateProfile() {
        UserPersistencePort persistencePort = mock(UserPersistencePort.class);
        AccountInternalUseCase accountInternalUseCase = mock(AccountInternalUseCase.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(persistencePort.existsByEmail("seller@sebook.local")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword");
        when(persistencePort.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        AdminUserService service = new AdminUserService(persistencePort, accountInternalUseCase, passwordEncoder);

        AdminUserUseCase.UserSummary summary = service.createStaff(new AdminUserUseCase.CreateStaffCommand(
                "Seller@SEBook.Local",
                "Nhân viên bán hàng",
                "Password123",
                "STAFF_SELLER"));

        assertThat(summary.id()).isEqualTo(10L);
        assertThat(summary.email()).isEqualTo("seller@sebook.local");
        assertThat(summary.role()).isEqualTo("STAFF_SELLER");
        verify(passwordEncoder).encode("Password123");
        verify(accountInternalUseCase).createDefaultProfile(10L);
    }
}
