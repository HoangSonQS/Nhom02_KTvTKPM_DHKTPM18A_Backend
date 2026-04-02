package iuh.fit.se.modules.order.domain;

public enum SagaStatus {
    INIT,
    STOCK_RESERVED,
    COUPON_RESERVED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
