package iuh.fit.se.modules.logistics.adapter.outbound.persistence;

import iuh.fit.se.modules.logistics.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaSupplierRepository extends JpaRepository<Supplier, Long> {
}
