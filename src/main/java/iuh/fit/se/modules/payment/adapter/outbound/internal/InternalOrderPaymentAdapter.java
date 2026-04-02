package iuh.fit.se.modules.payment.adapter.outbound.internal;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.payment.application.port.out.OrderPaymentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InternalOrderPaymentAdapter implements OrderPaymentPort {

    private final OrderInternalUseCase orderUseCase;

    @Override
    public Optional<OrderPaymentDto> findOrderForPayment(Long orderId) {
        try {
            OrderInternalUseCase.OrderResponse order = orderUseCase.getOrderById(orderId);
            return Optional.of(OrderPaymentDto.builder()
                    .orderId(order.getOrderId())
                    .totalAmount(order.getTotalAmount())
                    .status(order.getStatus())
                    .sagaStatus(order.getSagaStatus())
                    .build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void updateOrderPaid(Long orderId) {
        orderUseCase.markOrderAsPaid(orderId);
    }
}
