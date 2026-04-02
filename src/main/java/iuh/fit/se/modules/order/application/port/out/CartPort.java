package iuh.fit.se.modules.order.application.port.out;

import java.util.List;

public interface CartPort {
    CartDto getCartByUserId(Long userId);

    @lombok.Data
    @lombok.Builder
    class CartDto {
        private Long userId;
        private List<CartItemDto> items;
    }

    @lombok.Data
    @lombok.Builder
    class CartItemDto {
        private Long bookId;
        private String title;
        private java.math.BigDecimal price;
        private int quantity;
    }
}
