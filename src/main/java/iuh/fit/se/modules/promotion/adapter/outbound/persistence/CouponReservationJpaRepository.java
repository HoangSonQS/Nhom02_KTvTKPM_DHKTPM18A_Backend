package iuh.fit.se.modules.promotion.adapter.outbound.persistence;

import iuh.fit.se.modules.promotion.domain.CouponReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CouponReservationJpaRepository extends JpaRepository<CouponReservation, Long> {
    Optional<CouponReservation> findByReferenceId(String referenceId);
}
