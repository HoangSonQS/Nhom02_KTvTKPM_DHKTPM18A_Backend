package iuh.fit.se.modules.order.adapter.inbound.web;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderInternalUseCase orderUseCase;

    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.OrderResponse>> checkout(@RequestBody OrderInternalUseCase.CheckoutCommand command) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderInternalUseCase.OrderResponse response = orderUseCase.checkout(userId, command);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAuthority('ORDER_READ_SELF')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderInternalUseCase.OrderResponse>>> getMyOrders() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<OrderInternalUseCase.OrderResponse> response = orderUseCase.getMyOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAuthority('ORDER_READ_SELF')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.OrderResponse>> getMyOrderById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderInternalUseCase.OrderResponse response = orderUseCase.getMyOrderById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAuthority('ORDER_CANCEL_SELF')")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.OrderResponse>> cancelMyPendingOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        String reason = request == null ? "Customer cancelled pending order" : request.reason();
        OrderInternalUseCase.OrderResponse response = orderUseCase.cancelMyPendingOrder(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    record CancelOrderRequest(String reason) {
    }
}
