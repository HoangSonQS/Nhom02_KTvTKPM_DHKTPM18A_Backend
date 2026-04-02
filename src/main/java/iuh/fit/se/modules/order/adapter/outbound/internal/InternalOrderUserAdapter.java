package iuh.fit.se.modules.order.adapter.outbound.internal;

import iuh.fit.se.modules.auth.application.port.in.AuthInternalUseCase;
import iuh.fit.se.modules.order.application.port.out.OrderUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * InternalOrderUserAdapter — Implement OrderUserPort bằng cách gọi trực tiếp AuthInternalUseCase.
 */
@Component
@RequiredArgsConstructor
public class InternalOrderUserAdapter implements OrderUserPort {

    private final AuthInternalUseCase authUseCase;

    @Override
    public OrderUserPort.UserDto getUserDetails(Long userId) {
        AuthInternalUseCase.UserDetailsResponse user = authUseCase.getUserDetails(userId);
        
        return OrderUserPort.UserDto.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }
}
