package iuh.fit.se.modules.cart.adapter.inbound.web;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private Long id;
    private Long userId;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;

    @Data
    @Builder
    public static class CartItemResponse {
        private Long bookId;
        private String title;
        private int quantity;
        private BigDecimal price;
        private BigDecimal subTotal;
    }
}
