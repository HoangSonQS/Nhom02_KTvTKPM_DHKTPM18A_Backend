package iuh.fit.se.modules.catalog.adapter.outbound.persistence;

import iuh.fit.se.shared.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * BookJpaEntity — Đại diện bảng cat_book.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cat_book")
public class BookJpaEntity extends BaseEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Deprecated
    @Column(name = "deprecated_quantity", nullable = false)
    private int deprecatedQuantity;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "isbn", unique = true, length = 20)
    private String isbn;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "language", length = 50)
    private String language;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private String keywords; // Lưu dạng JSON string, Hibernate sẽ bind đúng kiểu jsonb

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "cover_type", length = 50)
    private String coverType;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "length")
    private Integer length;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "original_price", precision = 19, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "rating_count")
    private int ratingCount;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "image_public_id")
    private String imagePublicId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @ElementCollection
    @CollectionTable(name = "cat_book_category", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "category_id")
    @Builder.Default
    private Set<Long> categoryIds = new HashSet<>();
}
