package iuh.fit.se.modules.inventory.application.port.in;

public interface InventoryInternalUseCase {
    
    StockResult getAvailableStock(Long bookId);
    
    StockResult increaseStock(Long bookId, int amount, String referenceId);
    
    StockResult decreaseStock(Long bookId, int amount, String referenceId);

    // Bulk Operations for Saga
    java.util.List<StockResult> decreaseStockBulk(java.util.List<StockItemRequest> requests, String referenceId);

    java.util.List<StockResult> increaseStockBulk(java.util.List<StockItemRequest> requests, String referenceId);

    @lombok.Data
    @lombok.Builder
    class StockItemRequest {
        private Long bookId;
        private int amount;
    }
}
