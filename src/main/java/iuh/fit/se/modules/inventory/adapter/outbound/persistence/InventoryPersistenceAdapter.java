package iuh.fit.se.modules.inventory.adapter.outbound.persistence;

import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InventoryPersistenceAdapter implements InventoryPersistencePort {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final StockHistoryJpaRepository stockHistoryJpaRepository;

    @Override
    public Optional<InventoryStock> findStockByBookId(Long bookId) {
        return inventoryJpaRepository.findByBookId(bookId);
    }

    @Override
    public void saveStock(InventoryStock stock) {
        inventoryJpaRepository.save(stock);
    }

    @Override
    public int decreaseStockAtomically(Long bookId, int amount, Long version) {
        return inventoryJpaRepository.decreaseQuantityAtomically(bookId, amount, version);
    }

    @Override
    public int increaseStockAtomically(Long bookId, int amount, Long version) {
        return inventoryJpaRepository.increaseQuantityAtomically(bookId, amount, version);
    }

    @Override
    public Optional<StockHistory> findHistoryByReferenceId(String referenceId) {
        return stockHistoryJpaRepository.findByReferenceId(referenceId);
    }

    @Override
    public void saveHistory(StockHistory history) {
        stockHistoryJpaRepository.save(history);
    }

    @Override
    public int updateHistoryStatusAtomically(String referenceId, StockHistoryStatus oldStatus, StockHistoryStatus newStatus, LocalDateTime now) {
        return stockHistoryJpaRepository.updateStatusAtomically(referenceId, oldStatus, newStatus, now);
    }
}
