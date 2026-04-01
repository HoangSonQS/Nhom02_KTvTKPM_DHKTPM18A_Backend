package iuh.fit.se.modules.account.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AccountJpaRepository — Interface JPA nội bộ của module Account.
 */
@Repository
public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {

    Optional<AccountJpaEntity> findByUserId(Long userId);
}
