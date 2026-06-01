package iuh.fit.se.modules.promotion.application.service;

import iuh.fit.se.modules.promotion.application.port.in.PromotionApplicationResult;
import iuh.fit.se.modules.promotion.application.port.in.PromotionInternalUseCase;
import iuh.fit.se.modules.promotion.application.port.out.PromotionPersistencePort;
import iuh.fit.se.modules.promotion.domain.Coupon;
import iuh.fit.se.shared.event.realtime.DataChangedRealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import iuh.fit.se.modules.promotion.domain.CouponReservation;
import iuh.fit.se.modules.promotion.domain.CouponReservationStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService implements PromotionInternalUseCase {

    private final PromotionPersistencePort persistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getActiveCoupons() {
        return persistencePort.findActiveCoupons();
    }

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
            eventPublisher.publishEvent(DataChangedRealtimeEvent.of(
                    "COUPON_CHANGED",
                    "COUPON",
                    "Luot su dung ma khuyen mai da thay doi"
            ));

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

    @Override
    @Transactional
    public PromotionApplicationResult reserveCoupon(String code, BigDecimal orderTotal, String referenceId) {
        log.info("[{}] Reserving coupon {} for amount {}", referenceId, code, orderTotal);

        try {
            Optional<Coupon> couponOpt = persistencePort.findByCodeWithOptimisticLock(code);
            if (couponOpt.isEmpty()) {
                return PromotionApplicationResult.failure("Mã khuyến mãi không tồn tại", orderTotal, referenceId);
            }
            Coupon coupon = couponOpt.get();

            if (!coupon.isAppliable(orderTotal)) {
                return PromotionApplicationResult.failure("Mã không khả thi (hết hạn/lượt dùng hoặc không đủ điều kiện)", orderTotal, referenceId);
            }

            BigDecimal discount = coupon.calculateDiscount(orderTotal);

            // Tạo Reservation (Idempotent check by Unique reference_id at DB layer)
            CouponReservation reservation = CouponReservation.builder()
                    .coupon(coupon)
                    .referenceId(referenceId)
                    .status(CouponReservationStatus.RESERVED)
                    .expiresAt(LocalDateTime.now().plusMinutes(20))
                    .build();

            persistencePort.saveReservation(reservation);

            List<String> rules = List.of("SAGA_RESERVATION", "DISCOUNT_" + coupon.getDiscountType().name());
            return PromotionApplicationResult.success(orderTotal, discount, rules, referenceId);

        } catch (ObjectOptimisticLockingFailureException e) {
            return PromotionApplicationResult.failure("Mã vừa hết lượt dùng", orderTotal, referenceId);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Already reserved
            return persistencePort.findReservationByReferenceId(referenceId)
                    .map(res -> {
                        BigDecimal disc = res.getCoupon().calculateDiscount(orderTotal);
                        return PromotionApplicationResult.success(orderTotal, disc, List.of("ALREADY_RESERVED"), referenceId);
                    })
                    .orElse(PromotionApplicationResult.failure("Lỗi xung đột reservation", orderTotal, referenceId));
        } catch (Exception e) {
            log.error("[{}] Error reserving coupon: ", referenceId, e);
            return PromotionApplicationResult.failure("Lỗi hệ thống khi giữ mã.", orderTotal, referenceId);
        }
    }

    @Override
    @Transactional
    public void releaseCoupon(String referenceId) {
        persistencePort.findReservationByReferenceId(referenceId).ifPresent(res -> {
            if (res.getStatus() == CouponReservationStatus.RESERVED) {
                res.release();
                persistencePort.saveReservation(res);
                log.info("[{}] Coupon reservation released for ref: {}", referenceId, referenceId);
            }
        });
    }

    @Override
    @Transactional
    public void confirmCouponUsage(String referenceId) {
        persistencePort.findReservationByReferenceId(referenceId).ifPresent(res -> {
            // Idempotent check
            if (res.getStatus() == CouponReservationStatus.RESERVED) {
                res.confirm(); // Tăng usedCount của Coupon và set status reservation = CONFIRMED
                persistencePort.saveReservation(res);
                persistencePort.save(res.getCoupon());
                eventPublisher.publishEvent(DataChangedRealtimeEvent.of(
                        "COUPON_CHANGED",
                        "COUPON",
                        "Luot su dung ma khuyen mai da thay doi"
                ));
                log.info("[{}] Coupon usage confirmed for ref: {}", referenceId, referenceId);
            } else {
                log.info("[{}] Coupon already confirmed or released. Status: {}", referenceId, res.getStatus());
            }
        });
    }
}
