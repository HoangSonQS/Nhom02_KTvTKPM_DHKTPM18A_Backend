package iuh.fit.se.modules.auth.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA interface cho UserJpaEntity.
 * Internal — chỉ dùng trong persistence adapter, không expose ra ngoài module.
 */
interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
