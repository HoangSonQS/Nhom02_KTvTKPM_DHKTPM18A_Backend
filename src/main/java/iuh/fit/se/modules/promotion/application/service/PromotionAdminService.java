package iuh.fit.se.modules.promotion.application.service;

import iuh.fit.se.modules.promotion.application.port.in.PromotionAdminUseCase;
import iuh.fit.se.modules.promotion.application.port.out.PromotionPersistencePort;
import iuh.fit.se.modules.promotion.domain.Coupon;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.event.promotion.CouponCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionAdminService implements PromotionAdminUseCase {

    private final PromotionPersistencePort persistencePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return persistencePort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon getCouponById(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRM_COUPON_NOT_FOUND));
    }

    @Override
    @Transactional
    public Coupon createCoupon(CreateCouponCommand cmd) {
        if (persistencePort.existsByCode(cmd.code())) {
            throw new AppException(ErrorCode.PRM_COUPON_ALREADY_EXISTS);
        }

        Coupon coupon = Coupon.builder()
                .code(cmd.code().toUpperCase().trim())
                .name(cmd.name().trim())
                .description(cmd.description())
                .discountType(cmd.discountType())
                .discountValue(cmd.discountValue())
                .minOrderValue(cmd.minOrderValue())
                .maxDiscountValue(cmd.maxDiscountValue())
                .usageLimit(cmd.usageLimit())
                .usedCount(0)
                .startDate(cmd.startDate())
                .endDate(cmd.endDate())
                .isActive(cmd.isActive())
                .build();

        persistencePort.save(coupon);
        eventPublisher.publishEvent(new CouponCreatedEvent(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDiscountType().name(),
                coupon.getDiscountValue()
        ));
        log.info("[ADMIN] Created coupon: {}", coupon.getCode());
        return coupon;
    }

    @Override
    @Transactional
    public Coupon updateCoupon(Long id, UpdateCouponCommand cmd) {
        Coupon coupon = persistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRM_COUPON_NOT_FOUND));

        coupon.update(
                cmd.name(),
                cmd.description(),
                cmd.discountValue(),
                cmd.minOrderValue(),
                cmd.maxDiscountValue(),
                cmd.usageLimit(),
                cmd.startDate(),
                cmd.endDate(),
                cmd.isActive()
        );

        persistencePort.save(coupon);
        log.info("[ADMIN] Updated coupon id={}", id);
        return coupon;
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = persistencePort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRM_COUPON_NOT_FOUND));
        persistencePort.delete(coupon);
        log.info("[ADMIN] Deleted coupon id={}, code={}", id, coupon.getCode());
    }
}
