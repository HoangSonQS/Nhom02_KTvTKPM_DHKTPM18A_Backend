package iuh.fit.se.modules.auth.application.port.out;

import iuh.fit.se.modules.auth.domain.User;

import java.util.Optional;

/**
 * Outbound Port — giao diện mà Application Service dùng để truy cập DB.
 * Infrastructure Adapter (UserPersistenceAdapter) sẽ implement interface này.
 * Service KHÔNG bao giờ biết JPA hay PostgreSQL tồn tại.
 */
public interface UserPersistencePort {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);
}
