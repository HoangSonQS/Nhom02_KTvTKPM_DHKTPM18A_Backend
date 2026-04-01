package iuh.fit.se.modules.promotion.application.service;

import iuh.fit.se.modules.promotion.application.port.in.PromotionApplicationResult;
import iuh.fit.se.modules.promotion.application.port.in.PromotionInternalUseCase;
import iuh.fit.se.modules.promotion.application.port.out.PromotionPersistencePort;
import iuh.fit.se.modules.promotion.domain.Coupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService implements PromotionInternalUseCase {

    private final PromotionPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public PromotionApplicationResult validateCoupon(String code, BigDecimal orderTotal) {
        String traceId = UUID.randomUUID().toString();
        log.info("[{}] Validating coupon {} for amount {}", traceId, code, orderTotal);

        Optional<Coupon> couponOpt = persistencePort.findByCode(code);
        if (couponOpt.isEmpty()) {
            return PromotionApplicationResult.failure("Mã khuyến mãi không tồn tại", orderTotal, traceId);
        }

        Coupon coupon = couponOpt.get();
        if (!coupon.isAppliable(orderTotal)) {
            return PromotionApplicationResult.failure("Đơn hàng không đủ điều kiện hoặc mã đã hết hạn/lượt dùng", orderTotal, traceId);
        }

        BigDecimal discount = coupon.calculateDiscount(orderTotal);
        List<String> rules = List.of("DISCOUNT_" + coupon.getDiscountType().name());

        return PromotionApplicationResult.success(orderTotal, discount, rules, traceId);
    }

    @Override
    @Transactional
    public PromotionApplicationResult applyCoupon(String code, BigDecimal orderTotal, String orderId) {
        String traceId = "ORDER_" + orderId;
        log.info("[{}] Applying coupon {} for amount {}", traceId, code, orderTotal);

        try {
            // Sử dụng PESSIMISTIC lock nếu cần, nhưng bài toán này áp dụng OPTIMISTIC lock cho coupon -> nhẹ hơn
            Coupon coupon = persistencePort.findByCodeWithOptimisticLock(code)
                    .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại"));

            if (!coupon.isAppliable(orderTotal)) {
                return PromotionApplicationResult.failure("Đơn hàng không đủ điều kiện áp dụng mã", orderTotal, traceId);
            }

            BigDecimal discount = coupon.calculateDiscount(orderTotal);
            
            // Trừ lượt dùng
            coupon.incrementUsage();
            persistencePort.save(coupon);

            List<String> rules = List.of("DISCOUNT_" + coupon.getDiscountType().name());
            return PromotionApplicationResult.success(orderTotal, discount, rules, traceId);

        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("[{}] Optimistic lock failed for coupon {}", traceId, code);
            // Có thể retry hoặc trả về lỗi ngay để User thấy "Mã vừa hết lượt"
            return PromotionApplicationResult.failure("Mã khuyến mãi vừa hết lượt sử dụng, vui lòng thử lại.", orderTotal, traceId);
        } catch (Exception e) {
            log.error("[{}] Error applying coupon: ", traceId, e);
            return PromotionApplicationResult.failure("Lỗi hệ thống khi áp dụng mã.", orderTotal, traceId);
        }
    }
}
