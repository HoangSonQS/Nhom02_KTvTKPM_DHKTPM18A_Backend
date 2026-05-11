package iuh.fit.se.modules.returns.adapter.outbound.persistence;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.returns.application.port.out.OrderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderModuleIntegrationAdapter implements OrderQueryPort {

    private final OrderInternalUseCase orderUseCase;

    @Override
    public Optional<OrderDto> findOrderById(Long orderId) {
        try {
            OrderInternalUseCase.OrderResponse response = orderUseCase.getOrderById(orderId);
            return Optional.of(OrderDto.builder()
                    .orderId(response.getOrderId())
                    .customerId(response.getUserId())
                    .status(response.getFulfillmentStatus())
                    .deliveredAt(response.getUpdatedAt()) // FulfillmentStatus DELIVERED updatedAt is used as deliveredAt
                    .items(response.getItems().stream()
                            .map(item -> OrderItemDto.builder()
                                    .bookId(item.getBookId())
                                    .quantity(item.getQuantity())
                                    .price(item.getPriceAtPurchase())
                                    .build())
                            .collect(Collectors.toList()))
                    .build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
