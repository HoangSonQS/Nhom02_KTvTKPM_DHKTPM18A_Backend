package iuh.fit.se.modules.inventory.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryAdminUseCase {

    /**
     * Lấy danh sách toàn bộ tồn kho
     */
    List<InventoryResponse> getAllStocks();

    /**
     * Lấy thông tin tồn kho của một sách
     */
    InventoryResponse getStockByBookId(Long bookId);

    /**
     * Khởi tạo tồn kho ban đầu cho một sách mới
     */
    InventoryResponse initializeStock(Long bookId, int initialQuantity);

    /**
     * Cập nhật tồn kho (tăng số lượng)
     */
    InventoryResponse increaseStock(Long bookId, int amount);

    /**
     * Cập nhật tồn kho (giảm số lượng)
     */
    InventoryResponse decreaseStock(Long bookId, int amount);

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    record InventoryResponse(
            Long id,
            Long bookId,
            int quantity,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static InventoryResponse from(iuh.fit.se.modules.inventory.domain.InventoryStock s) {
            return new InventoryResponse(
                    s.getId(),
                    s.getBookId(),
                    s.getQuantity(),
                    s.getVersion(),
                    s.getCreatedAt(),
                    s.getUpdatedAt()
            );
        }
    }

    record AdjustStockRequest(
            @jakarta.validation.constraints.NotNull(message = "Số lượng điều chỉnh không được để trống")
            @jakarta.validation.constraints.Positive(message = "Số lượng điều chỉnh phải lớn hơn 0")
            Integer amount
    ) {}

    record InitializeStockRequest(
            @jakarta.validation.constraints.NotNull(message = "Book ID không được để trống")
            Long bookId,
            @jakarta.validation.constraints.NotNull(message = "Số lượng ban đầu không được để trống")
            @jakarta.validation.constraints.Positive(message = "Số lượng ban đầu phải lớn hơn 0")
            Integer initialQuantity
    ) {}
}
