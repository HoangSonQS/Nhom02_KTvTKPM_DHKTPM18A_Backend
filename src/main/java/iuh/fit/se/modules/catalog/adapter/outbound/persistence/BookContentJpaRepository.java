package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookContentJpaRepository extends JpaRepository<BookContentJpaEntity, Long> {
}
