package iuh.fit.se.modules.auth.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminUserUseCase {

    List<UserSummary> listUsers();

    UserSummary createStaff(CreateStaffCommand command);

    UserSummary lockUser(Long id);

    record CreateStaffCommand(
            String email,
            String fullName,
            String password,
            String role
    ) {}

    record UserSummary(
            Long id,
            String email,
            String fullName,
            String role,
            boolean enabled,
            LocalDateTime createdAt
    ) {}
}
