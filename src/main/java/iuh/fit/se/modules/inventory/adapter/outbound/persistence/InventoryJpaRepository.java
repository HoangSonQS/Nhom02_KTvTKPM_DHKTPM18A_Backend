package iuh.fit.se.modules.inventory.adapter.outbound.persistence;

import iuh.fit.se.modules.inventory.domain.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryJpaRepository extends JpaRepository<InventoryStock, Long> {

    Optional<InventoryStock> findByBookId(Long bookId);

    /**
     * 🔥 Atomic Update với Optimistic Locking và hard check quantity
     * Kết quả rowsAffected == 1 nghĩa là thành công.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE InventoryStock s SET s.quantity = s.quantity - :amount, s.version = s.version + 1 " +
           "WHERE s.bookId = :bookId AND s.version = :version AND s.quantity >= :amount")
    int decreaseQuantityAtomically(@Param("bookId") Long bookId, 
                                   @Param("amount") int amount, 
                                   @Param("version") Long version);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE InventoryStock s SET s.quantity = s.quantity + :amount, s.version = s.version + 1 " +
           "WHERE s.bookId = :bookId AND s.version = :version")
    int increaseQuantityAtomically(@Param("bookId") Long bookId, 
                                   @Param("amount") int amount, 
                                   @Param("version") Long version);
}
