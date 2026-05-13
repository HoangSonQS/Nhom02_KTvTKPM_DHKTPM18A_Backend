package iuh.fit.se.modules.inventory.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.modules.inventory.application.port.in.InventoryAdminUseCase;
import iuh.fit.se.modules.inventory.application.port.in.InventoryAdminUseCase.*;
import iuh.fit.se.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Admin", description = "APIs quản lý tồn kho dành cho Admin")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_READ')")
public class InventoryAdminController {

    private final InventoryAdminUseCase adminUseCase;

    @GetMapping
    @Operation(summary = "Lấy danh sách toàn bộ tồn kho")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllStocks() {
        List<InventoryResponse> stocks = adminUseCase.getAllStocks();
        return ResponseEntity.ok(ApiResponse.success("Danh sách tồn kho", stocks));
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Lấy thông tin tồn kho của một sách")
    public ResponseEntity<ApiResponse<InventoryResponse>> getStockByBookId(@PathVariable Long bookId) {
        InventoryResponse stock = adminUseCase.getStockByBookId(bookId);
        return ResponseEntity.ok(ApiResponse.success("Chi tiết tồn kho", stock));
    }

    // @PostMapping
    // @Operation(summary = "Khởi tạo tồn kho ban đầu cho một sách mới")
    // public ResponseEntity<ApiResponse<InventoryResponse>> initializeStock(@Valid
    // @RequestBody InitializeStockRequest req) {
    // InventoryResponse created = adminUseCase.initializeStock(req.bookId(),
    // req.initialQuantity());
    // return ResponseEntity.status(HttpStatus.CREATED)
    // .body(ApiResponse.success("Khởi tạo tồn kho thành công", created));
    // }

    @PutMapping("/{bookId}/increase")
    @Operation(summary = "Tăng số lượng tồn kho (Nhập hàng)")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_IMPORT_STOCK')")
    public ResponseEntity<ApiResponse<InventoryResponse>> increaseStock(
            @PathVariable Long bookId,
            @Valid @RequestBody AdjustStockRequest req) {
        InventoryResponse updated = adminUseCase.increaseStock(bookId, req.amount());
        return ResponseEntity.ok(ApiResponse.success("Tăng tồn kho thành công", updated));
    }

    @PutMapping("/{bookId}/decrease")
    @Operation(summary = "Giảm số lượng tồn kho (Hủy hàng/Xuất kho thủ công)")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('INVENTORY_IMPORT_STOCK')")
    public ResponseEntity<ApiResponse<InventoryResponse>> decreaseStock(
            @PathVariable Long bookId,
            @Valid @RequestBody AdjustStockRequest req) {
        InventoryResponse updated = adminUseCase.decreaseStock(bookId, req.amount());
        return ResponseEntity.ok(ApiResponse.success("Giảm tồn kho thành công", updated));
    }
}
