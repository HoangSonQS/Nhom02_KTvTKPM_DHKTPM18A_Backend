package iuh.fit.se.modules.order.application.port.in;

import lombok.Builder;
import lombok.Data;

public interface OrderInternalUseCase {

    Long checkout(Long userId, CheckoutCommand command);

    OrderResponse getOrderById(Long orderId);

    void markOrderAsPaid(Long orderId);

    @Data
    @Builder
    class OrderResponse {
        private Long orderId;
        private Long userId;
        private java.math.BigDecimal totalAmount;
        private String status;
        private String sagaStatus;
        private String requestId;
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
