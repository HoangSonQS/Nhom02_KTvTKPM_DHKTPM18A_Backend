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
import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String title;
    private String author;
    private String description;
    private BigDecimal price;
    
    @Deprecated
    private int deprecatedQuantity; // Sẽ được thay thế bởi Module Inventory
    
    private String imageUrl;
    private String imagePublicId;
    private boolean isActive;

    // Metadata bổ sung
    private String publisher;
    private String isbn;
    private Integer publicationYear;
    private String language;
    @Builder.Default
    private Set<String> keywords = new HashSet<>();

    // Thông số vật lý
    private Integer pageCount;
    private String coverType;
    private Integer weight; // gram
    private Integer length; // mm
    private Integer width; // mm
    private Integer height; // mm

    private BigDecimal originalPrice;
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;
    @Builder.Default
    private int ratingCount = 0;

    // Thành phần nội dung (tách bảng)
    private BookContent content;

    @Builder.Default
    private Set<Long> categoryIds = new HashSet<>();

    /**
     * Giảm tồn kho (Deprecated behavior - Move to Inventory soon).
     */
    public void decreaseStock(int amount) {
        if (amount <= 0) return;
        if (this.deprecatedQuantity < amount) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không đủ tồn kho cho sách: " + title);
        }
        this.deprecatedQuantity -= amount;
    }

    /**
     * Tăng tồn kho (Deprecated behavior).
     */
    public void increaseStock(int amount) {
        if (amount <= 0) return;
        this.deprecatedQuantity += amount;
    }

    /**
     * Cập nhật nội dung chi tiết. 
     * Encapsulated behavior: Chỉ Aggregate Root mới có quyền khởi tạo BookContent.
     */
    public void updateContent(String tableOfContents, String excerpt) {
        this.content = new BookContent(tableOfContents, excerpt);
    }

    public void addCategory(Long categoryId) {
        this.categoryIds.add(categoryId);
    }

    public void removeCategory(Long categoryId) {
        this.categoryIds.remove(categoryId);
    }

    public void updateBasicInfo(String title, String author, String description, BigDecimal price, Integer targetQuantity, Set<Long> categoryIds) {
        // Partial Update: chỉ cập nhật những trường được gửi (khác null)
        if (title != null) this.title = title;
        if (author != null) this.author = author;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (targetQuantity != null) this.deprecatedQuantity = targetQuantity;
        if (categoryIds != null) {
            this.categoryIds = new HashSet<>(categoryIds);
        }
    }

    /**
     * Cập nhật Metadata phống phú (Partial Update).
     * Trường nào null = giữ nguyên giá trị cũ trong DB.
     */
    public void updateMetadata(String publisher, String isbn, Integer publicationYear, String language, Set<String> keywords,
                               Integer pageCount, String coverType, Integer weight, Integer length, Integer width, Integer height,
                               BigDecimal originalPrice) {
        if (publisher != null) this.publisher = publisher;
        if (isbn != null) this.isbn = isbn;
        if (publicationYear != null) this.publicationYear = publicationYear;
        if (language != null) this.language = language;
        if (keywords != null) this.keywords = new HashSet<>(keywords);
        if (pageCount != null) this.pageCount = pageCount;
        if (coverType != null) this.coverType = coverType;
        if (weight != null) this.weight = weight;
        if (length != null) this.length = length;
        if (width != null) this.width = width;
        if (height != null) this.height = height;
        if (originalPrice != null) this.originalPrice = originalPrice;
    }

    public void updateImage(String imageUrl, String imagePublicId) {
        this.imageUrl = imageUrl;
        this.imagePublicId = imagePublicId;
    }

    /**
     * Đồng bộ số lượng tồn kho từ module Inventory.
     * Đây là phương thức hỗ trợ cho việc chuyển đổi kiến trúc.
     */
    public void syncQuantity(int quantity) {
        this.deprecatedQuantity = quantity;
    }

    public void updateRating(BigDecimal averageRating, int ratingCount) {
        this.averageRating = averageRating != null ? averageRating : BigDecimal.ZERO;
        this.ratingCount = Math.max(0, ratingCount);
    }
}
