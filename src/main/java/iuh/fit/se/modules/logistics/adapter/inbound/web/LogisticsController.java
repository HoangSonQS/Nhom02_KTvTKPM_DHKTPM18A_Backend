package iuh.fit.se.modules.logistics.adapter.inbound.web;

import iuh.fit.se.modules.logistics.adapter.inbound.web.dto.PurchaseOrderResponse;
import iuh.fit.se.modules.logistics.adapter.inbound.web.dto.SupplierResponse;
import iuh.fit.se.modules.logistics.application.port.in.LogisticsUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.security.SecurityUtils;
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
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_CREATE') or hasRole('ADMIN')")
    public ApiResponse<SupplierResponse> createSupplier(@RequestBody LogisticsUseCase.CreateSupplierCommand command) {
        return ApiResponse.success(SupplierResponse.from(logisticsUseCase.createSupplier(command)));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_READ_ALL') or hasRole('ADMIN')")
    public ApiResponse<List<SupplierResponse>> getAllSuppliers() {
        return ApiResponse.success(
                logisticsUseCase.getAllSuppliers().stream()
                        .map(SupplierResponse::from)
                        .toList()
        );
    }

    // --- Purchase Orders ---

    @PostMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_CREATE') or hasRole('ADMIN')")
    public ApiResponse<PurchaseOrderResponse> createPurchaseOrder(@RequestBody LogisticsUseCase.CreatePOCommand command) {
        return ApiResponse.success(
                PurchaseOrderResponse.from(
                        logisticsUseCase.createPurchaseOrder(command, SecurityUtils.getCurrentEmail())
                )
        );
    }

    @PostMapping("/purchase-orders/{id}/submit")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_CREATE') or hasRole('ADMIN')")
    public ApiResponse<PurchaseOrderResponse> submitPurchaseOrder(@PathVariable Long id) {
        return ApiResponse.success(
                PurchaseOrderResponse.from(
                        logisticsUseCase.submitPurchaseOrder(id, SecurityUtils.getCurrentRole())
                )
        );
    }

    @PostMapping("/purchase-orders/{id}/approve")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_APPROVE') or hasRole('ADMIN')")
    public ApiResponse<PurchaseOrderResponse> approvePurchaseOrder(@PathVariable Long id) {
        String adminName = SecurityUtils.getCurrentEmail();
        return ApiResponse.success(
                PurchaseOrderResponse.from(
                        logisticsUseCase.approvePurchaseOrder(id, SecurityUtils.getCurrentRole(), adminName)
                )
        );
    }

    @PostMapping("/purchase-orders/{id}/return")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_APPROVE') or hasRole('ADMIN')")
    public ApiResponse<PurchaseOrderResponse> returnPurchaseOrder(@PathVariable Long id, @RequestParam String reason) {
        return ApiResponse.success(
                PurchaseOrderResponse.from(
                        logisticsUseCase.returnPurchaseOrder(id, SecurityUtils.getCurrentRole(), reason)
                )
        );
    }

    @PostMapping("/purchase-orders/{id}/receive")
    @PreAuthorize("hasAuthority('INVENTORY_IMPORT_STOCK') or hasRole('ADMIN')")
    public ApiResponse<PurchaseOrderResponse> receivePurchaseOrder(@PathVariable Long id) {
        String receiverName = SecurityUtils.getCurrentEmail();
        return ApiResponse.success(
                PurchaseOrderResponse.from(
                        logisticsUseCase.receivePurchaseOrder(id, SecurityUtils.getCurrentRole(), receiverName)
                )
        );
    }

    @PostMapping("/purchase-orders/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_CREATE') or hasRole('ADMIN')")
    public ApiResponse<PurchaseOrderResponse> cancelPurchaseOrder(
            @PathVariable Long id,
            @RequestParam String reason) {
        String userName = SecurityUtils.getCurrentEmail();
        return ApiResponse.success(
                PurchaseOrderResponse.from(
                        logisticsUseCase.cancelPurchaseOrder(id, SecurityUtils.getCurrentRole(), userName, reason)
                )
        );
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_READ_ALL') or hasRole('ADMIN')")
    public ApiResponse<List<PurchaseOrderResponse>> getAllPurchaseOrders() {
        return ApiResponse.success(
                logisticsUseCase.getAllPurchaseOrders().stream()
                        .map(PurchaseOrderResponse::from)
                        .toList()
        );
    }

    @GetMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER_READ_ALL') or hasRole('ADMIN')")
    public ApiResponse<PurchaseOrderResponse> getPurchaseOrderById(@PathVariable Long id) {
        return ApiResponse.success(
                PurchaseOrderResponse.from(logisticsUseCase.getPurchaseOrderById(id))
        );
    }

    // --- Stock Adjustment ---

    @PostMapping("/stock-adjustments")
    @PreAuthorize("hasAuthority('STOCK_ADJUSTMENT_EXECUTE') or hasRole('ADMIN')")
    public ApiResponse<String> confirmStockAdjustment(@RequestBody LogisticsUseCase.StockAdjustmentCommand command) {
        logisticsUseCase.confirmStockAdjustment(command, SecurityUtils.getCurrentEmail());
        return ApiResponse.success("Yêu cầu điều chỉnh kho đã được gửi xử lý");
    }
}
