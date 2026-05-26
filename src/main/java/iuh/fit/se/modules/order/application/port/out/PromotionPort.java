package iuh.fit.se.modules.order.application.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionPort {
    PromotionResult reserveCoupon(String code, BigDecimal totalAmount, String referenceId);
    void releaseCoupon(String referenceId);
    void confirmCouponUsage(String referenceId);
    void reserveFlashSaleItems(List<FlashSaleItem> items);

    @lombok.Data
    @lombok.Builder
    class FlashSaleItem {
        private Long bookId;
        private int quantity;
    }

    @lombok.Data
    @lombok.Builder
    class PromotionResult {
        private boolean success;
        private BigDecimal discountAmount;
        private String message;
    }
}
