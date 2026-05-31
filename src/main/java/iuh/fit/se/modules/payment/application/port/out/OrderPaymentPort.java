package iuh.fit.se.modules.payment.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderPaymentPort {
    Optional<OrderPaymentDto> findOrderForPayment(Long orderId);
    void updateOrderPaid(Long orderId);
    void confirmPendingOrderAsCod(Long orderId, Long requesterId);

    @lombok.Data
    @lombok.Builder
    class OrderPaymentDto {
        private Long orderId;
        private Long customerId;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private String status;
        private String sagaStatus;
        private String requestId; // Coupon reservation referenceId
    }
}
