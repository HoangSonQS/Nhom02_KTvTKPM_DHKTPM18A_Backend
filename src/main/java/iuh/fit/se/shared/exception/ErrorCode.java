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
    INVALID_INPUT(4001, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),

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
    INV_IDEMPOTENCY_PENDING(4203, "Giao dịch đang được xử lý, vui lòng đợi", HttpStatus.ACCEPTED),

    // ===== Order Module (ORD_) =====
    ORD_NOT_FOUND(4304, "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    ORD_INVALID_STATUS(4300, "Trạng thái đơn hàng không hợp lệ cho thao tác này", HttpStatus.BAD_REQUEST),
    ORD_ALREADY_PAID(4301, "Đơn hàng đã được thanh toán", HttpStatus.CONFLICT),
    ORD_EXPIRED(4302, "Đơn hàng đã quá hạn thanh toán", HttpStatus.GONE),

    // ===== Payment Module (PAY_) =====
    PAY_SIGNATURE_INVALID(4400, "Chữ ký thanh toán không hợp lệ", HttpStatus.BAD_REQUEST),
    PAY_TRANSACTION_FAILED(4401, "Giao dịch thanh toán thất bại", HttpStatus.PAYMENT_REQUIRED),
    PAY_DUPLICATE_TXN(4402, "Giao dịch đã được ghi nhận trước đó", HttpStatus.CONFLICT),
    PAY_ORDER_MISMATCH(4403, "Thông tin đơn hàng không khớp", HttpStatus.BAD_REQUEST),

    // ===== Logistics Module (LOG_) =====
    LOG_INVALID_STATE_TRANSITION(4501, "Chuyển đổi trạng thái không hợp lệ", HttpStatus.CONFLICT),
    LOG_UNAUTHORIZED_STATE_TRANSITION(4502, "Bạn không có quyền thực hiện chuyển đổi trạng thái này", HttpStatus.FORBIDDEN),
    LOG_SUPPLIER_NOT_FOUND(4504, "Không tìm thấy nhà cung cấp", HttpStatus.NOT_FOUND),
    LOG_PO_NOT_FOUND(4505, "Không tìm thấy đơn hàng mua", HttpStatus.NOT_FOUND),

    // ===== Returns Module (RET_) =====
    RET_INVALID_STATE_TRANSITION(4601, "Chuyển trạng thái yêu cầu trả hàng không hợp lệ", HttpStatus.CONFLICT),
    RET_NOT_FOUND(4604, "Không tìm thấy yêu cầu trả hàng", HttpStatus.NOT_FOUND),
    RET_ORDER_NOT_DELIVERED(4605, "Chỉ có thể trả hàng cho đơn hàng đã giao thành công", HttpStatus.BAD_REQUEST),
    RET_EXCEEDED_RETURN_WINDOW(4606, "Đã quá thời hạn 7 ngày để yêu cầu trả hàng", HttpStatus.BAD_REQUEST),
    RET_INVALID_ITEMS(4607, "Danh sách hàng trả không hợp lệ", HttpStatus.BAD_REQUEST);


    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
