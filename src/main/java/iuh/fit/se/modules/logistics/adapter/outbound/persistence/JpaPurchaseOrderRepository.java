package iuh.fit.se.modules.logistics.adapter.outbound.persistence;

import iuh.fit.se.modules.logistics.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
}
