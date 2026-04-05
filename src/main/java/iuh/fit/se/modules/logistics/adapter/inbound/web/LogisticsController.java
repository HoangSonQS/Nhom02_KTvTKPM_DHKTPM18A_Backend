package iuh.fit.se.modules.logistics.adapter.inbound.web;

import iuh.fit.se.modules.logistics.application.port.in.LogisticsUseCase;
import iuh.fit.se.modules.logistics.domain.PurchaseOrder;
import iuh.fit.se.modules.logistics.domain.Supplier;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsUseCase logisticsUseCase;

    // --- Suppliers ---

    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF_WAREHOUSE')")
    public ApiResponse<Supplier> createSupplier(@RequestBody LogisticsUseCase.CreateSupplierCommand command) {
        return ApiResponse.success(logisticsUseCase.createSupplier(command));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF_WAREHOUSE')")
    public ApiResponse<List<Supplier>> getAllSuppliers() {
        return ApiResponse.success(logisticsUseCase.getAllSuppliers());
    }

    // --- Purchase Orders ---

    @PostMapping("/purchase-orders")
    @PreAuthorize("hasRole('STAFF_WAREHOUSE')")
    public ApiResponse<PurchaseOrder> createPurchaseOrder(@RequestBody LogisticsUseCase.CreatePOCommand command) {
        return ApiResponse.success(logisticsUseCase.createPurchaseOrder(command));
    }

    @PostMapping("/purchase-orders/{id}/submit")
    @PreAuthorize("hasRole('STAFF_WAREHOUSE')")
    public ApiResponse<PurchaseOrder> submitPurchaseOrder(@PathVariable Long id) {
        // Trong thực tế, lấy username từ SecurityContext
        return ApiResponse.success(logisticsUseCase.submitPurchaseOrder(id, "STAFF_WAREHOUSE"));
    }

    @PostMapping("/purchase-orders/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PurchaseOrder> approvePurchaseOrder(@PathVariable Long id, @RequestParam String adminName) {
        return ApiResponse.success(logisticsUseCase.approvePurchaseOrder(id, "ADMIN", adminName));
    }

    @PostMapping("/purchase-orders/{id}/return")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PurchaseOrder> returnPurchaseOrder(@PathVariable Long id, @RequestParam String reason) {
        return ApiResponse.success(logisticsUseCase.returnPurchaseOrder(id, "ADMIN", reason));
    }

    @PostMapping("/purchase-orders/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF_WAREHOUSE')")
    public ApiResponse<PurchaseOrder> receivePurchaseOrder(@PathVariable Long id, @RequestParam String receiverName) {
        return ApiResponse.success(logisticsUseCase.receivePurchaseOrder(id, "STAFF_WAREHOUSE", receiverName));
    }

    @PostMapping("/purchase-orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF_WAREHOUSE')")
    public ApiResponse<PurchaseOrder> cancelPurchaseOrder(
            @PathVariable Long id, 
            @RequestParam String userName, 
            @RequestParam String reason) {
        // Logic check role nội bộ trong PO domain sẽ handle permission chi tiết
        return ApiResponse.success(logisticsUseCase.cancelPurchaseOrder(id, "USER", userName, reason));
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF_WAREHOUSE')")
    public ApiResponse<List<PurchaseOrder>> getAllPurchaseOrders() {
        return ApiResponse.success(logisticsUseCase.getAllPurchaseOrders());
    }

    @GetMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF_WAREHOUSE')")
    public ApiResponse<PurchaseOrder> getPurchaseOrderById(@PathVariable Long id) {
        return ApiResponse.success(logisticsUseCase.getPurchaseOrderById(id));
    }

    // --- Stock Adjustment ---

    @PostMapping("/stock-adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF_WAREHOUSE')")
    public ApiResponse<String> confirmStockAdjustment(@RequestBody LogisticsUseCase.StockAdjustmentCommand command) {
        logisticsUseCase.confirmStockAdjustment(command);
        return ApiResponse.success("Yêu cầu điều chỉnh kho đã được gửi xử lý");
    }
}
