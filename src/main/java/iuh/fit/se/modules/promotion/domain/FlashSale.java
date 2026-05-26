package iuh.fit.se.modules.promotion.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "prm_flash_sale")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FlashSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "sale_quantity", nullable = false)
    private int saleQuantity;

    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(Long bookId, int saleQuantity, int discountPercent, LocalDateTime startAt, LocalDateTime endAt, boolean active) {
        this.bookId = bookId;
        this.saleQuantity = saleQuantity;
        this.discountPercent = discountPercent;
        this.startAt = startAt;
        this.endAt = endAt;
        this.active = active;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("So luong mua phai lon hon 0");
        }
        if (saleQuantity < quantity) {
            throw new IllegalStateException("Flash Sale khong du so luong");
        }
        this.saleQuantity -= quantity;
    }
}
