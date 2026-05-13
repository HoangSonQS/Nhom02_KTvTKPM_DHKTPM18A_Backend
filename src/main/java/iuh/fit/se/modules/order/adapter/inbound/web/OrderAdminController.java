package iuh.fit.se.modules.order.adapter.inbound.web;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase.UpdateFulfillmentStatusCommand;
import iuh.fit.se.modules.order.domain.FulfillmentStatus;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderInternalUseCase orderUseCase;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_READ_ALL')")
    public ResponseEntity<ApiResponse<List<OrderInternalUseCase.AdminOrderResponse>>> searchOrders(
            @RequestParam(required = false) FulfillmentStatus status,
            @RequestParam(required = false) String customerKeyword) {
        OrderInternalUseCase.AdminOrderSearchCriteria criteria = OrderInternalUseCase.AdminOrderSearchCriteria.builder()
                .status(status)
                .customerKeyword(customerKeyword)
                .build();
        return ResponseEntity.ok(ApiResponse.success(orderUseCase.searchAdminOrders(criteria)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_READ_ALL')")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.AdminOrderResponse>> getOrderDetails(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderUseCase.getAdminOrderById(id)));
    }

    /**
     * Generic status update — Admin/Staff gửi FulfillmentStatus target.
     * Đây là endpoint tổng quát; các endpoint cụ thể bên dưới là shortcut tiện
     * dụng.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_UPDATE_STATUS')")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody UpdateFulfillmentStatusCommand command) {
        return ResponseEntity.ok(ApiResponse.success(orderUseCase.updateOrderStatus(id, command)));
    }

    /**
     * CONFIRMED → PROCESSING: Bắt đầu xử lý đơn hàng.
     */
    @PutMapping("/{id}/process")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_UPDATE_STATUS')")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.OrderResponse>> processOrder(@PathVariable Long id) {
        UpdateFulfillmentStatusCommand command = UpdateFulfillmentStatusCommand.builder()
                .newStatus(FulfillmentStatus.PROCESSING)
                .reason("Đơn hàng đã được xác nhận và đang xử lý")
                .build();
        return ResponseEntity.ok(ApiResponse.success(orderUseCase.updateOrderStatus(id, command)));
    }

    /**
     * PROCESSING → DELIVERING: Bắt đầu giao hàng.
     */
    @PutMapping("/{id}/ship")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_UPDATE_STATUS')")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.OrderResponse>> shipOrder(@PathVariable Long id) {
        UpdateFulfillmentStatusCommand command = UpdateFulfillmentStatusCommand.builder()
                .newStatus(FulfillmentStatus.DELIVERING)
                .reason("Đơn hàng đang được giao")
                .build();
        return ResponseEntity.ok(ApiResponse.success(orderUseCase.updateOrderStatus(id, command)));
    }

    /**
     * DELIVERING → DELIVERED: Xác nhận giao hàng thành công (terminal positive
     * state).
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_UPDATE_STATUS')")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.OrderResponse>> completeOrder(@PathVariable Long id) {
        UpdateFulfillmentStatusCommand command = UpdateFulfillmentStatusCommand.builder()
                .newStatus(FulfillmentStatus.DELIVERED)
                .reason("Đơn hàng đã giao thành công")
                .build();
        return ResponseEntity.ok(ApiResponse.success(orderUseCase.updateOrderStatus(id, command)));
    }

    /**
     * Force-cancel đơn hàng với lý do rõ ràng.
     * Sử dụng cancelOrder() — bỏ qua transition guard, nhưng chặn DELIVERED và
     * CANCELLED.
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_UPDATE_STATUS')")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest request) {
        String reason = (request != null && request.reason() != null)
                ? request.reason()
                : "Đơn hàng bị hủy bởi Admin/Staff";
        return ResponseEntity.ok(ApiResponse.success(orderUseCase.cancelOrder(id, reason)));
    }

    record CancelOrderRequest(String reason) {
    }
}
