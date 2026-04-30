package iuh.fit.se.modules.inventory.application.port.in;

import java.util.List;
import java.util.Map;

public interface InventoryInternalUseCase {
    
    StockResult getAvailableStock(Long bookId);
    Map<Long, Integer> getAvailableStocks(List<Long> bookIds);
    
    StockResult increaseStock(Long bookId, int amount, String referenceId);
    
    StockResult decreaseStock(Long bookId, int amount, String referenceId);

    // Bulk Operations for Saga
    List<StockResult> decreaseStockBulk(List<StockItemRequest> requests, String referenceId);

    List<StockResult> increaseStockBulk(List<StockItemRequest> requests, String referenceId);

    @lombok.Data
    @lombok.Builder
    class StockItemRequest {
        private Long bookId;
        private int amount;
    }
}
