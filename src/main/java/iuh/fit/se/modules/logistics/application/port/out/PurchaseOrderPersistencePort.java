package iuh.fit.se.modules.logistics.application.port.out;

import iuh.fit.se.modules.logistics.domain.PurchaseOrder;
import iuh.fit.se.modules.logistics.domain.PurchaseOrderHistory;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderPersistencePort {
    PurchaseOrder save(PurchaseOrder purchaseOrder);
    Optional<PurchaseOrder> findById(Long id);
    List<PurchaseOrder> findAll();
    void saveHistory(PurchaseOrderHistory history);
}
