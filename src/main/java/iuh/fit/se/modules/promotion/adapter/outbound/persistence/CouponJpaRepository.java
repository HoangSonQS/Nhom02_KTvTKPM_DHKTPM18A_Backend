package iuh.fit.se.modules.promotion.adapter.outbound.persistence;

import iuh.fit.se.modules.promotion.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeWithOptimisticLock(@Param("code") String code);
    
    Optional<Coupon> findByCode(String code);

    @Query("""
            SELECT c FROM Coupon c
            WHERE c.isActive = true
              AND (c.startDate IS NULL OR c.startDate <= :now)
              AND (c.endDate IS NULL OR c.endDate >= :now)
              AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)
            ORDER BY CASE WHEN c.endDate IS NULL THEN 1 ELSE 0 END, c.endDate ASC, c.createdAt DESC
            """)
    List<Coupon> findActiveCoupons(@Param("now") LocalDateTime now);

    boolean existsByCode(String code);
}
