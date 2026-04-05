package iuh.fit.se.modules.logistics.adapter.outbound.persistence;

import iuh.fit.se.modules.logistics.application.port.out.PurchaseOrderPersistencePort;
import iuh.fit.se.modules.logistics.domain.PurchaseOrder;
import iuh.fit.se.modules.logistics.domain.PurchaseOrderHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PurchaseOrderPersistenceAdapter implements PurchaseOrderPersistencePort {
    private final JpaPurchaseOrderRepository poRepository;
    private final JpaPurchaseOrderHistoryRepository historyRepository;

    @Override
    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        return poRepository.save(purchaseOrder);
    }

    @Override
    public Optional<PurchaseOrder> findById(Long id) {
        return poRepository.findById(id);
    }

    @Override
    public List<PurchaseOrder> findAll() {
        return poRepository.findAll();
    }

    @Override
    public void saveHistory(PurchaseOrderHistory history) {
        historyRepository.save(history);
    }
}
