package iuh.fit.se.modules.order.adapter.outbound.internal;

import iuh.fit.se.modules.account.application.port.in.AccountUseCase;
import iuh.fit.se.modules.auth.application.port.in.AuthInternalUseCase;
import iuh.fit.se.modules.order.application.port.out.OrderUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * InternalOrderUserAdapter — Implement OrderUserPort bằng cách gọi trực tiếp
 * AuthInternalUseCase và AccountUseCase để lấy đầy đủ thông tin User.
 */
@Component
@RequiredArgsConstructor
public class InternalOrderUserAdapter implements OrderUserPort {

    private final AuthInternalUseCase authUseCase;
    private final AccountUseCase accountUseCase;

    @Override
    public OrderUserPort.UserDto getUserDetails(Long userId) {
        AuthInternalUseCase.UserDetailsResponse user = authUseCase.getUserDetails(userId);
        AccountUseCase.AccountProfileResponse account = accountUseCase.getProfile(userId);

        String defaultAddress = account.addresses().stream()
                .filter(AccountUseCase.AddressResponse::isDefault)
                .map(a -> a.street() + ", " + a.ward() + ", " + a.city())
                .findFirst()
                .orElse(null);
        String defaultAddressPhone = account.addresses().stream()
                .filter(AccountUseCase.AddressResponse::isDefault)
                .map(AccountUseCase.AddressResponse::phoneNumber)
                .filter(phone -> phone != null && !phone.isBlank())
                .findFirst()
                .orElse(null);

        return OrderUserPort.UserDto.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(defaultAddressPhone != null ? defaultAddressPhone : account.phoneNumber())
                .defaultAddress(defaultAddress)
                .build();
    }
}
