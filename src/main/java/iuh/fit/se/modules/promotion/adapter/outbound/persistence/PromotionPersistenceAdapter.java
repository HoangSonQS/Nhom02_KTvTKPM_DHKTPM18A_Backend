package iuh.fit.se.modules.promotion.adapter.outbound.persistence;

import iuh.fit.se.modules.promotion.application.port.out.PromotionPersistencePort;
import iuh.fit.se.modules.promotion.domain.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PromotionPersistenceAdapter implements PromotionPersistencePort {

    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Optional<Coupon> findByCode(String code) {
        return couponJpaRepository.findByCode(code);
    }

    @Override
    public Optional<Coupon> findByCodeWithOptimisticLock(String code) {
        return couponJpaRepository.findByCodeWithOptimisticLock(code);
    }

    @Override
    public void save(Coupon coupon) {
        couponJpaRepository.save(coupon);
    }
}
