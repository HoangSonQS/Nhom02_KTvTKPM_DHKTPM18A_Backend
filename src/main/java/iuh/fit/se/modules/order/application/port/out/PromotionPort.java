package iuh.fit.se.modules.order.application.port.out;

import java.math.BigDecimal;

public interface PromotionPort {
    PromotionResult reserveCoupon(String code, BigDecimal totalAmount, String referenceId);
    void releaseCoupon(String referenceId);
    void confirmCouponUsage(String referenceId);

    @lombok.Data
    @lombok.Builder
    class PromotionResult {
        private boolean success;
        private BigDecimal discountAmount;
        private String message;
    }
}
