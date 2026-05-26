package iuh.fit.se.modules.cart.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crt_cart_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "price_at_add_time", nullable = false)
    private BigDecimal priceAtAddTime;

    @Column(name = "title_snapshot")
    private String titleSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static CartItem create(Cart cart, Long bookId, int quantity, BigDecimal price, String title) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
        return CartItem.builder()
                .cart(cart)
                .bookId(bookId)
                .quantity(quantity)
                .priceAtAddTime(price)
                .titleSnapshot(title)
                .build();
    }

    public void updateQuantity(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Số lượng không được âm");
        }
        this.quantity = newQuantity;
    }
    
    public void addQuantity(int amount) {
        this.quantity += amount;
    }

    public void replacePrice(BigDecimal price, String title) {
        this.priceAtAddTime = price;
        this.titleSnapshot = title;
    }

    public BigDecimal getSubTotal() {
        return priceAtAddTime.multiply(BigDecimal.valueOf(quantity));
    }
}
