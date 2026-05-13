package iuh.fit.se.modules.auth.application.service;

import iuh.fit.se.modules.auth.application.port.in.AdminUserUseCase;
import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.modules.auth.domain.Role;
import iuh.fit.se.modules.auth.domain.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    @Test
    void givenUsers_whenListUsers_thenReturnSafeSummariesWithoutPassword() {
        UserPersistencePort persistencePort = mock(UserPersistencePort.class);
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

        AdminUserService service = new AdminUserService(persistencePort);

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

        AdminUserService service = new AdminUserService(persistencePort);

        AdminUserUseCase.UserSummary summary = service.lockUser(2L);

        assertThat(summary.enabled()).isFalse();
        verify(persistencePort).save(user);
    }
}
