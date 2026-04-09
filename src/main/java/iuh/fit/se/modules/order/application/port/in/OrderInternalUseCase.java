package iuh.fit.se.modules.order.application.port.in;

import lombok.Builder;
import lombok.Data;

public interface OrderInternalUseCase {

    Long checkout(Long userId, CheckoutCommand command);

    OrderResponse getOrderById(Long orderId);

    void markOrderAsPaid(Long orderId);

    void processReturnCompleted(Long orderId);

    @Data
    @Builder
    class OrderResponse {
        private Long orderId;
        private Long userId;
        private java.math.BigDecimal totalAmount;
        private String status;
        private String sagaStatus;
        private String requestId;
        private java.time.LocalDateTime updatedAt;
        private java.util.List<OrderItemResponse> items;
    }

    @Data
    @Builder
    class OrderItemResponse {
        private Long bookId;
        private int quantity;
        private java.math.BigDecimal priceAtPurchase;
    }

    @Data
    @Builder
    class CheckoutCommand {
        private String requestId;
        private String shippingAddress;
        private String customerPhone;
        private String couponCode;
    }
}
