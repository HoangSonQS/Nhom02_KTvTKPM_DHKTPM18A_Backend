package iuh.fit.se.modules.promotion.application.port.in;

import iuh.fit.se.modules.promotion.domain.Coupon;

import java.util.List;

public interface PromotionInternalUseCase {

    List<Coupon> getActiveCoupons();
    
    /**
     * Dùng cho Cart để kiểm tra mã (soft check), không trừ lượt dùng.
     */
    PromotionApplicationResult validateCoupon(String code, java.math.BigDecimal orderTotal);

    // Saga operations
    PromotionApplicationResult reserveCoupon(String code, java.math.BigDecimal orderTotal, String referenceId);
    
    void releaseCoupon(String referenceId);
    
    void confirmCouponUsage(String referenceId);

    /**
     * Dùng cho Checkout để áp dụng mã chính thức và trừ lượt dùng (trong 1 transaction).
     */
    @Deprecated
    PromotionApplicationResult applyCoupon(String code, java.math.BigDecimal orderTotal, String orderId);
}
