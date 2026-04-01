package iuh.fit.se.shared.exception;

import lombok.Getter;

/**
 * Runtime exception dùng chung toàn hệ thống.
 * Mang theo ErrorCode để GlobalExceptionHandler biết cách map ra HTTP response.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
