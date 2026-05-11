package iuh.fit.se.modules.order.domain.exception;

import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;

public class InvalidOrderTransitionException extends AppException {
    public InvalidOrderTransitionException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }
}
