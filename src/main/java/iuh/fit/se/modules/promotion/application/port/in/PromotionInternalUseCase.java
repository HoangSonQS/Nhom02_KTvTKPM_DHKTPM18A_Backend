package iuh.fit.se.modules.promotion.application.port.in;

import java.math.BigDecimal;

public interface PromotionInternalUseCase {
    
    /**
     * Dùng cho Cart để kiểm tra mã (soft check), không trừ lượt dùng.
     */
    PromotionApplicationResult validateCoupon(String code, BigDecimal orderTotal);

    /**
     * Dùng cho Checkout để áp dụng mã chính thức và trừ lượt dùng (trong 1 transaction).
     */
    PromotionApplicationResult applyCoupon(String code, BigDecimal orderTotal, String orderId);
}
