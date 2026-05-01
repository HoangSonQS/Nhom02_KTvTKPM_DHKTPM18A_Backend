package iuh.fit.se.modules.promotion.adapter.outbound.persistence;

import iuh.fit.se.modules.promotion.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeWithOptimisticLock(@Param("code") String code);
    
    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);
}
