package iuh.fit.se.modules.auth.application.service;

import iuh.fit.se.modules.auth.application.port.in.AdminUserUseCase;
import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService implements AdminUserUseCase {

    private final UserPersistencePort userPersistencePort;

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userPersistencePort.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public UserSummary lockUser(Long id) {
        var user = userPersistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND));
        user.disable();
        return toSummary(userPersistencePort.save(user));
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
