package iuh.fit.se.modules.account.application.port.in;

/**
 * AccountInternalUseCase — Inbound Port (Internal API).
 * Dùng để module Auth gọi sang.
 */
public interface AccountInternalUseCase {

    /**
     * Tạo profile mặc định khi user đăng ký thành công.
     */
    void createDefaultProfile(Long userId);
}
