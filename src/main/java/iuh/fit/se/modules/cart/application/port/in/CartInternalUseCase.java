package iuh.fit.se.modules.cart.application.port.in;

import iuh.fit.se.modules.cart.domain.Cart;
import lombok.Builder;
import lombok.Data;


public interface CartInternalUseCase {

    Cart getCartByUserId(Long userId);

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
