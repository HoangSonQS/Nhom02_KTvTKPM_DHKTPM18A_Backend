package iuh.fit.se.modules.order.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    DELIVERING,
    CANCELLED,
    COMPLETED,
    RETURNED,
    PARTIAL_RETURNED
}
