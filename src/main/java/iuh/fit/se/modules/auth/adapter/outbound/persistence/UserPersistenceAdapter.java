package iuh.fit.se.modules.auth.adapter.outbound.persistence;

import iuh.fit.se.modules.auth.application.port.out.UserPersistencePort;
import iuh.fit.se.modules.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Persistence Adapter — implement UserPersistencePort bằng JPA.
 * Đây là nơi duy nhất chịu trách nhiệm mapping giữa Domain Object và JPA Entity.
 * AuthService không bao giờ biết class này tồn tại.
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toJpaEntity(user);
        UserJpaEntity saved = userJpaRepository.save(entity);
        return toDomain(saved);
    }

    // ===== Mapping Methods =====

    private User toDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .fullName(entity.getFullName())
                .role(entity.getRole())
                .enabled(entity.isEnabled())
                .build();
    }

    private UserJpaEntity toJpaEntity(User user) {
        return UserJpaEntity.builder()
                .email(user.getEmail())
                .password(user.getPassword())
                .fullName(user.getFullName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
}
