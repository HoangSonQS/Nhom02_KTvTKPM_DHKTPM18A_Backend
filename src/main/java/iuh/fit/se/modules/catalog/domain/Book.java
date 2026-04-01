package iuh.fit.se.modules.catalog.domain;

import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Book — Aggregate Root cho module Book.
 * Chứa logic quản lý tồn kho (Inventory behavior).
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Book {
    @Setter
    private Long id;
    private String title;
    private String author;
    private String description;
    private BigDecimal price;
    private int quantity;
    private String imageUrl;
    private String imagePublicId;
    private boolean isActive;

    @Builder.Default
    private Set<Long> categoryIds = new HashSet<>();

    /**
     * Giảm tồn kho. Throws exception nếu không đủ hàng.
     */
    public void decreaseStock(int amount) {
        if (amount <= 0) return;
        if (this.quantity < amount) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không đủ tồn kho cho sách: " + title);
        }
        this.quantity -= amount;
    }

    /**
     * Tăng tồn kho.
     */
    public void increaseStock(int amount) {
        if (amount <= 0) return;
        this.quantity += amount;
    }

    public void addCategory(Long categoryId) {
        this.categoryIds.add(categoryId);
    }

    public void removeCategory(Long categoryId) {
        this.categoryIds.remove(categoryId);
    }

    public void updateBasicInfo(String title, String author, String description, BigDecimal price, int targetQuantity, Set<Long> categoryIds) {
        this.title = title;
        this.author = author;
        this.description = description;
        this.price = price;
        this.quantity = targetQuantity;
        if (categoryIds != null) {
            this.categoryIds = new HashSet<>(categoryIds);
        } else {
            this.categoryIds.clear();
        }
    }

    public void updateImage(String imageUrl, String imagePublicId) {
        this.imageUrl = imageUrl;
        this.imagePublicId = imagePublicId;
    }
}
