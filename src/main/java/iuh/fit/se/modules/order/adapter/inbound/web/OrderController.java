package iuh.fit.se.modules.order.adapter.inbound.web;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderInternalUseCase orderUseCase;

    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Long>> checkout(@RequestBody OrderInternalUseCase.CheckoutCommand command) {
        Long userId = getCurrentUserId();
        Long orderId = orderUseCase.checkout(userId, command);
        return ResponseEntity.ok(ApiResponse.success(orderId));
    }

    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Không tìm thấy thông tin xác thực");
        }
        return (Long) auth.getCredentials();
    }
}
