package iuh.fit.se.modules.inventory.application.port.in;

public interface InventoryInternalUseCase {
    
    StockResult getAvailableStock(Long bookId);
    
    StockResult increaseStock(Long bookId, int amount, String referenceId);
    
    StockResult decreaseStock(Long bookId, int amount, String referenceId);
}
