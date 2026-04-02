package iuh.fit.se.modules.auth.application.port.in;

import lombok.Builder;
import lombok.Getter;

/**
 * AuthInternalUseCase — Inbound Port (Internal API).
 * Dùng để các module khác (vd: Order) gọi sang lấy thông tin User.
 */
public interface AuthInternalUseCase {

    UserDetailsResponse getUserDetails(Long userId);

    @Getter
    @Builder
    class UserDetailsResponse {
        private Long id;
        private String email;
        private String fullName;
        private String role;
    }
}
