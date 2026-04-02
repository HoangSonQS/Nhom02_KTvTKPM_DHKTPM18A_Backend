package iuh.fit.se.modules.order.application.port.out;

import lombok.Builder;
import lombok.Getter;

/**
 * OrderUserPort — Outbound Port để gọi sang module Account/Auth lấy thông tin User.
 */
public interface OrderUserPort {

    UserDto getUserDetails(Long userId);

    @Getter
    @Builder
    class UserDto {
        private String fullName;
        private String email;
    }
}
