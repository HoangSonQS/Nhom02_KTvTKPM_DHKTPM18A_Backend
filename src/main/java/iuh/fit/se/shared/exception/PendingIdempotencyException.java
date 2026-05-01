package iuh.fit.se.shared.exception;

/**
 * Exception thrown when an idempotent request is currently being processed by another thread.
 * This signals the caller to retry after a short delay (short-polling).
 */
public class PendingIdempotencyException extends RuntimeException {
    public PendingIdempotencyException() {
        super("Giao dịch đang được xử lý, vui lòng đợi...");
    }
}
