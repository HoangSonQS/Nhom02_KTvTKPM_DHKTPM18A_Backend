package iuh.fit.se.modules.cart.adapter.inbound.web;

import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartInternalUseCase cartUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<CartInternalUseCase.CartResponse>> getCart() {
        Long userId = getCurrentUserId();
        CartInternalUseCase.CartResponse cart = cartUseCase.getCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<String>> addItem(@RequestBody CartInternalUseCase.AddItemCommand command) {
        Long userId = getCurrentUserId();
        cartUseCase.addItem(userId, command);
        return ResponseEntity.ok(ApiResponse.success("Thêm vào giỏ hàng thành công"));
    }

    @PutMapping("/items")
    public ResponseEntity<ApiResponse<String>> updateQuantity(
            @RequestBody CartInternalUseCase.UpdateQuantityCommand command) {
        Long userId = getCurrentUserId();
        cartUseCase.updateItemQuantity(userId, command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công"));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<ApiResponse<String>> removeItem(@PathVariable Long bookId) {
        Long userId = getCurrentUserId();
        cartUseCase.removeItem(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearCart() {
        Long userId = getCurrentUserId();
        cartUseCase.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã làm trống giỏ hàng"));
    }



    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Không tìm thấy thông tin xác thực");
        }
        return (Long) auth.getCredentials();
    }
}
