package iuh.fit.se.modules.cart.application.port.in;

import lombok.Builder;
import lombok.Data;


public interface CartInternalUseCase {

    CartResponse getCartByUserId(Long userId);

    @lombok.Data
    @lombok.Builder
    class CartResponse {
        private Long userId;
        private java.util.List<CartItemResponse> items;
        private java.math.BigDecimal totalAmount;
    }

    @lombok.Data
    @lombok.Builder
    class CartItemResponse {
        private Long bookId;
        private String title;
        private java.math.BigDecimal price;
        private int quantity;
    }

    void addItem(Long userId, AddItemCommand command);

    void updateItemQuantity(Long userId, UpdateQuantityCommand command);

    void removeItem(Long userId, Long bookId);

    void clearCart(Long userId);

    @Data
    @Builder
    class AddItemCommand {
        private Long bookId;
        private int quantity;
    }

    @Data
    @Builder
    class UpdateQuantityCommand {
        private Long bookId;
        private int quantity;
    }
}
