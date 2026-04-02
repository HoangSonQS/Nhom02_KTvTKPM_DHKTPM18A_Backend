package iuh.fit.se.modules.order.adapter.outbound.internal;

import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.modules.order.application.port.out.CartPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InternalCartAdapter implements CartPort {

    private final CartInternalUseCase cartUseCase;

    @Override
    public CartPort.CartDto getCartByUserId(Long userId) {
        iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase.CartResponse cart = cartUseCase.getCartByUserId(userId);
        
        return CartPort.CartDto.builder()
                .userId(userId)
                .items(cart.getItems().stream()
                        .map(item -> CartPort.CartItemDto.builder()
                                .bookId(item.getBookId())
                                .title(item.getTitle())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
