package iuh.fit.se.modules.promotion.application.port.out;

import iuh.fit.se.modules.promotion.domain.Coupon;
import java.util.Optional;

public interface PromotionPersistencePort {
    Optional<Coupon> findByCode(String code);
    Optional<Coupon> findByCodeWithOptimisticLock(String code);
    void save(Coupon coupon);

    // Reservation methods
    void saveReservation(iuh.fit.se.modules.promotion.domain.CouponReservation reservation);
    java.util.Optional<iuh.fit.se.modules.promotion.domain.CouponReservation> findReservationByReferenceId(String referenceId);
}
