package iuh.fit.se.shared.audit.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, Long> {
    List<AuditLogJpaEntity> findTop100ByOrderByCreatedAtDesc();

    List<AuditLogJpaEntity> findTop100ByRoleInOrderByCreatedAtDesc(List<String> roles);
}
