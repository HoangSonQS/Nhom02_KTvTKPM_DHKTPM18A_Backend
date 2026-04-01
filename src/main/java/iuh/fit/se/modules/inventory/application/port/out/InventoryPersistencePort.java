package iuh.fit.se.modules.inventory.application.port.out;

import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InventoryPersistencePort {
    
    // Inventory Stock
    Optional<InventoryStock> findStockByBookId(Long bookId);
    void saveStock(InventoryStock stock);
    int decreaseStockAtomically(Long bookId, int amount, Long version);
    int increaseStockAtomically(Long bookId, int amount, Long version);

    // Stock History (Idempotency)
    Optional<StockHistory> findHistoryByReferenceId(String referenceId);
    void saveHistory(StockHistory history);
    int updateHistoryStatusAtomically(String referenceId, StockHistoryStatus oldStatus, StockHistoryStatus newStatus, LocalDateTime now);
}
