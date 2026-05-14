package iuh.fit.se.modules.auth.application.service;

import iuh.fit.se.modules.auth.application.port.in.AdminUserUseCase;
import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.modules.auth.domain.Role;
import iuh.fit.se.modules.auth.domain.User;
import iuh.fit.se.modules.account.application.port.in.AccountInternalUseCase;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService implements AdminUserUseCase {

    private final UserPersistencePort userPersistencePort;
    private final AccountInternalUseCase accountInternalUseCase;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userPersistencePort.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "ADMIN_CREATE_STAFF")
    public UserSummary createStaff(CreateStaffCommand command) {
        Role role = parseStaffRole(command.role());
        String email = command.email().trim().toLowerCase();
        if (userPersistencePort.existsByEmail(email)) {
            throw new AppException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
        }

        User staff = User.create(
                email,
                passwordEncoder.encode(command.password()),
                command.fullName().trim(),
                role);
        User saved = userPersistencePort.save(staff);
        accountInternalUseCase.createDefaultProfile(saved.getId());
        return toSummary(saved);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "ADMIN_LOCK_USER")
    public UserSummary lockUser(Long id) {
        var user = userPersistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND));
        user.disable();
        return toSummary(userPersistencePort.save(user));
    }

    private Role parseStaffRole(String rawRole) {
        try {
            Role role = Role.valueOf(rawRole);
            if (role == Role.STAFF_SELLER || role == Role.STAFF_WAREHOUSE) {
                return role;
            }
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // Fall through to a domain-facing validation error.
        }
        throw new AppException(ErrorCode.INVALID_INPUT, "Chỉ được tạo tài khoản STAFF_SELLER hoặc STAFF_WAREHOUSE");
    }

    private UserSummary toSummary(iuh.fit.se.modules.auth.domain.User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.isEnabled()
        );
    }
}
