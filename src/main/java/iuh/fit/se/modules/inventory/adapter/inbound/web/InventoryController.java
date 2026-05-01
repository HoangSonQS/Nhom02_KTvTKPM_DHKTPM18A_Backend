package iuh.fit.se.modules.inventory.adapter.inbound.web;

import iuh.fit.se.modules.inventory.application.port.in.InventoryAdminUseCase;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryAdminUseCase adminUseCase;

    // ─── Admin Endpoints ─────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/inventory
     * Lấy danh sách toàn bộ tồn kho (Admin only).
     */
    @GetMapping("/api/v1/admin/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllStocks() {
        List<InventoryResponse> stocks = adminUseCase.getAllStocks()
                .stream()
                .map(InventoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Danh sách tồn kho", stocks));
    }

    /**
     * GET /api/v1/admin/inventory/{bookId}
     * Lấy thông tin tồn kho của một sách (Admin only).
     */
    @GetMapping("/api/v1/admin/inventory/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> getStockByBookId(@PathVariable Long bookId) {
        InventoryStock stock = adminUseCase.getStockByBookId(bookId);
        return ResponseEntity.ok(ApiResponse.success("Chi tiết tồn kho", InventoryResponse.from(stock)));
    }

    /**
     * POST /api/v1/admin/inventory
     * Khởi tạo tồn kho ban đầu cho một sách mới (Admin only).
     */
    @PostMapping("/api/v1/admin/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> initializeStock(@Valid @RequestBody InitializeStockRequest req) {
        InventoryStock created = adminUseCase.initializeStock(req.bookId(), req.initialQuantity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo tồn kho thành công", InventoryResponse.from(created)));
    }

    /**
     * PUT /api/v1/admin/inventory/{bookId}/increase
     * Tăng số lượng tồn kho (Nhập hàng - Admin only).
     */
    @PutMapping("/api/v1/admin/inventory/{bookId}/increase")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> increaseStock(
            @PathVariable Long bookId,
            @Valid @RequestBody AdjustStockRequest req) {
        InventoryStock updated = adminUseCase.increaseStock(bookId, req.amount());
        return ResponseEntity.ok(ApiResponse.success("Tăng tồn kho thành công", InventoryResponse.from(updated)));
    }

    /**
     * PUT /api/v1/admin/inventory/{bookId}/decrease
     * Giảm số lượng tồn kho (Hủy hàng/Xuất kho thủ công - Admin only).
     */
    @PutMapping("/api/v1/admin/inventory/{bookId}/decrease")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> decreaseStock(
            @PathVariable Long bookId,
            @Valid @RequestBody AdjustStockRequest req) {
        InventoryStock updated = adminUseCase.decreaseStock(bookId, req.amount());
        return ResponseEntity.ok(ApiResponse.success("Giảm tồn kho thành công", InventoryResponse.from(updated)));
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    record InventoryResponse(
            Long id,
            Long bookId,
            int quantity,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        static InventoryResponse from(InventoryStock s) {
            return new InventoryResponse(
                    s.getId(), s.getBookId(), s.getQuantity(), s.getVersion(),
                    s.getCreatedAt(), s.getUpdatedAt()
            );
        }
    }

    record InitializeStockRequest(
            @NotNull(message = "Book ID không được để trống") Long bookId,
            @NotNull(message = "Số lượng ban đầu không được để trống") @Positive(message = "Số lượng ban đầu phải lớn hơn 0") Integer initialQuantity
    ) {}

    record AdjustStockRequest(
            @NotNull(message = "Số lượng điều chỉnh không được để trống") @Positive(message = "Số lượng điều chỉnh phải lớn hơn 0") Integer amount
    ) {}
}
