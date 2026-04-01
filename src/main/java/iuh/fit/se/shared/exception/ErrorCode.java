package iuh.fit.se.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enum tập trung tất cả mã lỗi nghiệp vụ của hệ thống.
 * Mỗi module thêm mã lỗi vào đây theo prefix domain (AUTH_, ACC_, CAT_...).
 */
@Getter
public enum ErrorCode {

    // ===== Global =====
    INTERNAL_ERROR(5000, "Lỗi hệ thống nội bộ", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED(4000, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(4004, "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(4003, "Không có quyền truy cập", HttpStatus.FORBIDDEN),

    // ===== Auth Module (AUTH_) =====
    AUTH_INVALID_CREDENTIALS(4010, "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_EXPIRED(4011, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID(4012, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    AUTH_EMAIL_ALREADY_EXISTS(4013, "Email đã được đăng ký", HttpStatus.CONFLICT),
    AUTH_USER_NOT_FOUND(4014, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    AUTH_ACCOUNT_DISABLED(4015, "Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),

    // ===== Inventory Module (INV_) =====
    INV_STOCK_NOT_FOUND(4200, "Không tìm thấy tồn kho cho sách này", HttpStatus.NOT_FOUND),
    INV_OUT_OF_STOCK(4201, "Hết hàng hoặc không đủ số lượng", HttpStatus.CONFLICT),
    INV_STOCK_LOCK_TIMEOUT(4202, "Kho đang bận xử lý, vui lòng thử lại sau giây lát", HttpStatus.CONFLICT),
    INV_IDEMPOTENCY_PENDING(4203, "Giao dịch đang được xử lý, vui lòng đợi", HttpStatus.ACCEPTED);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
