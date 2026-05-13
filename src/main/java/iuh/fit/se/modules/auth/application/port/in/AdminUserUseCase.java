package iuh.fit.se.modules.auth.application.port.in;

import java.util.List;

public interface AdminUserUseCase {

    List<UserSummary> listUsers();

    UserSummary lockUser(Long id);

    record UserSummary(
            Long id,
            String email,
            String fullName,
            String role,
            boolean enabled
    ) {}
}
