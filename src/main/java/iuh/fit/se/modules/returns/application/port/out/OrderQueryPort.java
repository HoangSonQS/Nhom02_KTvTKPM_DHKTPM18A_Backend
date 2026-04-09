package iuh.fit.se.modules.returns.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderQueryPort {
    Optional<OrderDto> findOrderById(Long orderId);

    @lombok.Getter
    @lombok.Builder
    class OrderDto {
        private Long orderId;
        private Long customerId;
        private String status;
        private LocalDateTime deliveredAt;
        private List<OrderItemDto> items;
    }

    @lombok.Getter
    @lombok.Builder
    class OrderItemDto {
        private Long bookId;
        private int quantity;
        private BigDecimal price;
    }
}
