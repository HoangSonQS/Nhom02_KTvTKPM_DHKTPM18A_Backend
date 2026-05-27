package iuh.fit.se.modules.promotion.application.port.in;

import iuh.fit.se.modules.promotion.domain.Coupon;
import iuh.fit.se.modules.promotion.domain.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PromotionAdminUseCase {

    List<Coupon> getAllCoupons();

    Coupon getCouponById(Long id);

    Coupon createCoupon(CreateCouponCommand command);

    Coupon updateCoupon(Long id, UpdateCouponCommand command);

    void deleteCoupon(Long id);

    // ─── Inner Command Records ────────────────────────────────────────────────

    record CreateCouponCommand(
            String code,
            String name,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minOrderValue,
            BigDecimal maxDiscountValue,
            Integer usageLimit,
            LocalDateTime startDate,
            LocalDateTime endDate,
            boolean isActive
    ) {}

    record UpdateCouponCommand(
            String name,
            String description,
            BigDecimal discountValue,
            BigDecimal minOrderValue,
            BigDecimal maxDiscountValue,
            Integer usageLimit,
            LocalDateTime startDate,
            LocalDateTime endDate,
            boolean isActive
    ) {}
}
