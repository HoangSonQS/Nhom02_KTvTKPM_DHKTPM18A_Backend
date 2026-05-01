package iuh.fit.se.modules.inventory.application.port.in;

import iuh.fit.se.modules.inventory.domain.InventoryStock;

import java.util.List;

public interface InventoryAdminUseCase {

    /**
     * Lấy danh sách toàn bộ tồn kho
     */
    List<InventoryStock> getAllStocks();

    /**
     * Lấy thông tin tồn kho của một sách
     */
    InventoryStock getStockByBookId(Long bookId);

    /**
     * Khởi tạo tồn kho ban đầu cho một sách mới
     */
    InventoryStock initializeStock(Long bookId, int initialQuantity);

    /**
     * Cập nhật tồn kho (tăng số lượng)
     */
    InventoryStock increaseStock(Long bookId, int amount);

    /**
     * Cập nhật tồn kho (giảm số lượng)
     */
    InventoryStock decreaseStock(Long bookId, int amount);
}
