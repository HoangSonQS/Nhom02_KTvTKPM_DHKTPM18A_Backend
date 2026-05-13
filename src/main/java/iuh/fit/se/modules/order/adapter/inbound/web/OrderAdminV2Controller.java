package iuh.fit.se.modules.order.adapter.inbound.web;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.order.domain.FulfillmentStatus;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/admin/orders")
@RequiredArgsConstructor
public class OrderAdminV2Controller {

    private final OrderInternalUseCase orderUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderInternalUseCase.AdminOrderResponse>> getOrderDetails(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderUseCase.getAdminOrderById(id)));
    }
}
