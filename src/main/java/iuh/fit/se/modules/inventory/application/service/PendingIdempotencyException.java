package iuh.fit.se.modules.inventory.application.service;

import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;

public class PendingIdempotencyException extends AppException {
    public PendingIdempotencyException() {
        super(ErrorCode.INV_IDEMPOTENCY_PENDING);
    }
}
