package iuh.fit.se.modules.cart.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "crt_cart")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Cart create(Long userId) {
        return Cart.builder()
                .userId(userId)
                .build();
    }

    public void addItem(Long bookId, int quantity, BigDecimal price, String title, int maxPerItem) {
        Optional<CartItem> existingItem = items.stream()
                .filter(item -> item.getBookId().equals(bookId))
                .findFirst();

        if (existingItem.isPresent()) {
            int newQuantity = existingItem.get().getQuantity() + quantity;
            if (newQuantity > maxPerItem) {
                throw new IllegalStateException("Số lượng sách vượt quá giới hạn tối đa cho phép (" + maxPerItem + ")");
            }
            existingItem.get().addQuantity(quantity);
        } else {
            if (quantity > maxPerItem) {
                throw new IllegalStateException("Số lượng sách vượt quá giới hạn tối đa cho phép (" + maxPerItem + ")");
            }
            items.add(CartItem.create(this, bookId, quantity, price, title));
        }
    }

    public void updateItemQuantity(Long bookId, int newQuantity, int maxPerItem) {
        if (newQuantity > maxPerItem) {
            throw new IllegalStateException("Số lượng sách vượt quá giới hạn tối đa cho phép (" + maxPerItem + ")");
        }

        if (newQuantity <= 0) {
            removeItem(bookId);
            return;
        }

        items.stream()
                .filter(item -> item.getBookId().equals(bookId))
                .findFirst()
                .ifPresent(item -> item.updateQuantity(newQuantity));
    }

    public void removeItem(Long bookId) {
        items.removeIf(item -> item.getBookId().equals(bookId));
    }

    public void clearItems() {
        items.clear();
    }

    public BigDecimal calculateTotal() {
        return items.stream()
                .map(CartItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
