package iuh.fit.se.modules.inventory.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inv_stock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class InventoryStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false, unique = true)
    private Long bookId;

    @Column(nullable = false)
    private int quantity;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static InventoryStock create(Long bookId, int initialQuantity) {
        if (initialQuantity < 0) {
            throw new IllegalArgumentException("Số lượng ban đầu không được âm");
        }
        return InventoryStock.builder()
                .bookId(bookId)
                .quantity(initialQuantity)
                .build();
    }

    public void increase(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng tăng phải lớn hơn 0");
        }
        this.quantity += amount;
    }

    public void decrease(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng giảm phải lớn hơn 0");
        }
        if (this.quantity < amount) {
            throw new IllegalStateException("Hết hàng (Out of stock)");
        }
        this.quantity -= amount;
    }
}
