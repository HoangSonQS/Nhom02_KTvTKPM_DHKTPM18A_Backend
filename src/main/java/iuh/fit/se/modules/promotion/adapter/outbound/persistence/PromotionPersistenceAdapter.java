package iuh.fit.se.modules.promotion.adapter.outbound.persistence;

import iuh.fit.se.modules.promotion.application.port.out.PromotionPersistencePort;
import iuh.fit.se.modules.promotion.domain.Coupon;
import iuh.fit.se.modules.promotion.domain.CouponReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
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
    public List<Coupon> findAll() {
        return couponJpaRepository.findAll();
    }

    @Override
    public List<Coupon> findActiveCoupons() {
        return couponJpaRepository.findActiveCoupons(LocalDateTime.now());
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return couponJpaRepository.findById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return couponJpaRepository.existsByCode(code);
    }

    @Override
    public void delete(Coupon coupon) {
        couponJpaRepository.delete(coupon);
    }

    @Override
    public void saveReservation(CouponReservation reservation) {
        reservationJpaRepository.save(reservation);
    }

    @Override
    public Optional<CouponReservation> findReservationByReferenceId(String referenceId) {
        return reservationJpaRepository.findByReferenceId(referenceId);
    }
}
