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
    private final CouponReservationJpaRepository reservationJpaRepository;

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

    @Override
    public void saveReservation(iuh.fit.se.modules.promotion.domain.CouponReservation reservation) {
        reservationJpaRepository.save(reservation);
    }

    @Override
    public java.util.Optional<iuh.fit.se.modules.promotion.domain.CouponReservation> findReservationByReferenceId(String referenceId) {
        return reservationJpaRepository.findByReferenceId(referenceId);
    }
}
