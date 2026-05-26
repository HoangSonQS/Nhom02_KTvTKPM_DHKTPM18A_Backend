package iuh.fit.se.modules.order.adapter.outbound.persistence;

import iuh.fit.se.modules.order.domain.Order;
import iuh.fit.se.modules.order.domain.FulfillmentStatus;
import iuh.fit.se.modules.order.domain.SagaStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByRequestId(String requestId);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "items")
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<Order> findAllByFulfillmentStatusOrderByCreatedAtDesc(FulfillmentStatus fulfillmentStatus);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT o FROM Order o WHERE o.sagaStatus NOT IN (:statuses) AND o.createdAt < :before")
    List<Order> findAbandonedOrders(@Param("statuses") List<SagaStatus> statuses, 
                                     @Param("before") LocalDateTime before, 
                                     Pageable pageable);

    @Modifying
    @Query("UPDATE Order o SET o.sagaStatus = :nextStatus WHERE o.id = :id AND o.sagaStatus = :currentStatus")
    int updateSagaStatusAtomic(@Param("id") Long id, 
                               @Param("currentStatus") SagaStatus currentStatus, 
                               @Param("nextStatus") SagaStatus nextStatus);

    @Query("""
            SELECT item.bookId AS bookId,
                   MAX(item.bookTitle) AS title,
                   SUM(item.quantity) AS quantitySold,
                   SUM(item.priceAtPurchase * item.quantity) AS revenue
            FROM Order o
            JOIN o.items item
            WHERE o.fulfillmentStatus IN :statuses
            GROUP BY item.bookId
            ORDER BY SUM(item.quantity) DESC
            """)
    List<TopSellingBookRow> findTopSellingBooks(@Param("statuses") List<FulfillmentStatus> statuses, Pageable pageable);

    @Query("""
            SELECT item.bookId AS bookId,
                   MAX(item.bookTitle) AS title,
                   SUM(item.quantity) AS quantitySold,
                   SUM(item.priceAtPurchase * item.quantity) AS revenue
            FROM Order o
            JOIN o.items item
            WHERE o.fulfillmentStatus IN :statuses
            GROUP BY item.bookId
            ORDER BY SUM(item.quantity) DESC
            """)
    List<TopSellingBookRow> findBookSales(@Param("statuses") List<FulfillmentStatus> statuses);

    @Query("""
            SELECT item.bookId AS bookId,
                   MAX(item.bookTitle) AS title,
                   SUM(item.quantity) AS quantitySold,
                   SUM(item.priceAtPurchase * item.quantity) AS revenue
            FROM Order o
            JOIN o.items item
            WHERE o.fulfillmentStatus IN :statuses
              AND o.updatedAt >= :fromDate
            GROUP BY item.bookId
            ORDER BY SUM(item.quantity) DESC
            """)
    List<TopSellingBookRow> findBookSalesFrom(
            @Param("statuses") List<FulfillmentStatus> statuses,
            @Param("fromDate") LocalDateTime fromDate);

    @Query("""
            SELECT item.bookId AS bookId,
                   MAX(item.bookTitle) AS title,
                   SUM(item.quantity) AS quantitySold,
                   SUM(item.priceAtPurchase * item.quantity) AS revenue
            FROM Order o
            JOIN o.items item
            WHERE o.fulfillmentStatus IN :statuses
              AND o.updatedAt < :toDate
            GROUP BY item.bookId
            ORDER BY SUM(item.quantity) DESC
            """)
    List<TopSellingBookRow> findBookSalesTo(
            @Param("statuses") List<FulfillmentStatus> statuses,
            @Param("toDate") LocalDateTime toDate);

    @Query("""
            SELECT item.bookId AS bookId,
                   MAX(item.bookTitle) AS title,
                   SUM(item.quantity) AS quantitySold,
                   SUM(item.priceAtPurchase * item.quantity) AS revenue
            FROM Order o
            JOIN o.items item
            WHERE o.fulfillmentStatus IN :statuses
              AND o.updatedAt >= :fromDate
              AND o.updatedAt < :toDate
            GROUP BY item.bookId
            ORDER BY SUM(item.quantity) DESC
            """)
    List<TopSellingBookRow> findBookSalesBetween(
            @Param("statuses") List<FulfillmentStatus> statuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    interface TopSellingBookRow {
        Long getBookId();
        String getTitle();
        Long getQuantitySold();
        BigDecimal getRevenue();
    }
}
