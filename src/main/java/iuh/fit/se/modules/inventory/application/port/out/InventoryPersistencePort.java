package iuh.fit.se.modules.inventory.application.port.out;

import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InventoryPersistencePort {
    
    void updateStockAtomic(Long bookId, Integer adjustmentQuantity);
    void saveProcessedEvent(java.util.UUID eventId);
    boolean existsProcessedEvent(java.util.UUID eventId);

    // Inventory Stock
    Optional<InventoryStock> findStockByBookId(Long bookId);
    List<InventoryStock> findStocksByBookIds(List<Long> bookIds);
    List<InventoryStock> findAllStocks();
    void saveStock(InventoryStock stock);
    int setStockQuantity(Long bookId, int quantity);
    int decreaseStockAtomically(Long bookId, int amount, Long version);
    int increaseStockAtomically(Long bookId, int amount, Long version);

    // Stock History (Idempotency)
    Optional<StockHistory> findHistoryByReferenceId(String referenceId);
    void saveHistory(StockHistory history);
    int updateHistoryStatusAtomically(String referenceId, StockHistoryStatus oldStatus, StockHistoryStatus newStatus, LocalDateTime now);
}
