package iuh.fit.se.modules.inventory.adapter.outbound.persistence;

import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StockHistoryJpaRepository extends JpaRepository<StockHistory, Long> {

    Optional<StockHistory> findByReferenceId(String referenceId);

    /**
     * 🔥 CAS-style update để khôi phục các trạng thái PENDING/FAILED
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StockHistory h SET h.status = :newStatus, h.lockedAt = :now " +
           "WHERE h.referenceId = :refId AND h.status = :oldStatus")
    int updateStatusAtomically(@Param("refId") String referenceId, 
                               @Param("oldStatus") StockHistoryStatus oldStatus, 
                               @Param("newStatus") StockHistoryStatus newStatus,
                               @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE StockHistory h SET h.status = iuh.fit.se.modules.inventory.domain.StockHistoryStatus.FAILED, " +
           "h.processedAt = :now, h.resultData = '{\"error\":\"Stale PENDING cleaned by job\"}' " +
           "WHERE h.status = iuh.fit.se.modules.inventory.domain.StockHistoryStatus.PENDING " +
           "AND h.lockedAt < :threshold AND h.processedAt IS NULL")
    int markStalePendingAsFailed(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);

    default int markStalePendingAsFailed(LocalDateTime threshold) {
        return markStalePendingAsFailed(threshold, LocalDateTime.now());
    }
}
