package iuh.fit.se.modules.promotion.adapter.outbound.persistence;

import iuh.fit.se.modules.promotion.domain.FlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashSaleJpaRepository extends JpaRepository<FlashSale, Long> {

    List<FlashSale> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT sale
            FROM FlashSale sale
            WHERE sale.active = true
              AND sale.saleQuantity > :saleQuantity
              AND sale.startAt >= :dayStart
              AND sale.startAt < :nextDayStart
              AND sale.endAt > :now
            ORDER BY sale.startAt ASC, sale.endAt ASC
            """)
    List<FlashSale> findVisibleToday(
            @Param("saleQuantity") int saleQuantity,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("nextDayStart") LocalDateTime nextDayStart,
            @Param("now") LocalDateTime now
    );

    Optional<FlashSale> findFirstByBookIdAndActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanOrderByDiscountPercentDesc(
            Long bookId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    Optional<FlashSale> findFirstByBookIdAndActiveTrueAndSaleQuantityGreaterThanAndStartAtLessThanEqualAndEndAtGreaterThanOrderByDiscountPercentDesc(
            Long bookId,
            int saleQuantity,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    @Query("""
            SELECT COUNT(sale) > 0
            FROM FlashSale sale
            WHERE sale.bookId = :bookId
              AND sale.active = true
              AND sale.startAt < :endAt
              AND sale.endAt > :startAt
            """)
    boolean existsOverlappingActiveSale(
            @Param("bookId") Long bookId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            SELECT COUNT(sale) > 0
            FROM FlashSale sale
            WHERE sale.id <> :id
              AND sale.bookId = :bookId
              AND sale.active = true
              AND sale.startAt < :endAt
              AND sale.endAt > :startAt
            """)
    boolean existsOverlappingActiveSaleExcludingId(
            @Param("id") Long id,
            @Param("bookId") Long bookId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}
