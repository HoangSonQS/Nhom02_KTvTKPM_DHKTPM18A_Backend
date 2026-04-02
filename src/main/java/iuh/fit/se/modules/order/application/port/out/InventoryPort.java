package iuh.fit.se.modules.order.application.port.out;

import java.util.List;

public interface InventoryPort {
    void decreaseStockBulk(List<StockItem> items, String referenceId);
    void increaseStockBulk(List<StockItem> items, String referenceId);

    @lombok.Data
    @lombok.Builder
    class StockItem {
        private Long bookId;
        private int quantity;
    }
}
