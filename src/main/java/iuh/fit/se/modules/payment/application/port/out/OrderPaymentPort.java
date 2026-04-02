package iuh.fit.se.modules.payment.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderPaymentPort {
    Optional<OrderPaymentDto> findOrderForPayment(Long orderId);
    void updateOrderPaid(Long orderId); // Sửa tên tý cho rõ

    @lombok.Data
    @lombok.Builder
    class OrderPaymentDto {
        private Long orderId;
        private BigDecimal totalAmount;
        private String status;
        private String sagaStatus;
    }
}
