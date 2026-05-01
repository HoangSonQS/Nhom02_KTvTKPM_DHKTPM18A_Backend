package iuh.fit.se.modules.promotion.application.port.out;

import iuh.fit.se.modules.promotion.domain.Coupon;
import iuh.fit.se.modules.promotion.domain.CouponReservation;

import java.util.List;
import java.util.Optional;

public interface PromotionPersistencePort {
    // Customer/internal validation
    Optional<Coupon> findByCode(String code);
    Optional<Coupon> findByCodeWithOptimisticLock(String code);
    void save(Coupon coupon);

    // Admin CRUD
    List<Coupon> findAll();
    Optional<Coupon> findById(Long id);
    boolean existsByCode(String code);
    void delete(Coupon coupon);

    // Reservation methods
    void saveReservation(CouponReservation reservation);
    Optional<CouponReservation> findReservationByReferenceId(String referenceId);
}
