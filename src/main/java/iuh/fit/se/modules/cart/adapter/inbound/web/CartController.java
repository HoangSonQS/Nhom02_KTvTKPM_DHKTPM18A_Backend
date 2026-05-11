package iuh.fit.se.modules.cart.adapter.inbound.web;

import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartInternalUseCase cartUseCase;

    @PreAuthorize("hasAuthority('CART_READ_SELF')")
    @GetMapping
    public ResponseEntity<ApiResponse<CartInternalUseCase.CartResponse>> getCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        CartInternalUseCase.CartResponse cart = cartUseCase.getCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PreAuthorize("hasAuthority('CART_WRITE_SELF')")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<String>> addItem(@RequestBody CartInternalUseCase.AddItemCommand command) {
        Long userId = SecurityUtils.getCurrentUserId();
        cartUseCase.addItem(userId, command);
        return ResponseEntity.ok(ApiResponse.success("Thêm vào giỏ hàng thành công"));
    }

    @PreAuthorize("hasAuthority('CART_WRITE_SELF')")
    @PutMapping("/items")
    public ResponseEntity<ApiResponse<String>> updateQuantity(
            @RequestBody CartInternalUseCase.UpdateQuantityCommand command) {
        Long userId = SecurityUtils.getCurrentUserId();
        cartUseCase.updateItemQuantity(userId, command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công"));
    }

    @PreAuthorize("hasAuthority('CART_WRITE_SELF')")
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<ApiResponse<String>> removeItem(@PathVariable Long bookId) {
        Long userId = SecurityUtils.getCurrentUserId();
        cartUseCase.removeItem(userId, bookId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng"));
    }

    @PreAuthorize("hasAuthority('CART_WRITE_SELF')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        cartUseCase.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã làm trống giỏ hàng"));
    }
}
