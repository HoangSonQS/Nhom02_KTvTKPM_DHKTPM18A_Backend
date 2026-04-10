package iuh.fit.se.modules.catalog.application.port.in;

import lombok.Builder;
import java.math.BigDecimal;
import java.util.Set;

/**
 * BookDTO — Data Transfer Object dùng để giao tiếp liên module.
 * Đảm bảo không leak Domain Entity "Book" ra ngoài Catalog module.
 */
@Builder
public record BookDTO(
    Long id,
    String title,
    String author,
    String description,
    BigDecimal price,
    int quantity,
    String imageUrl,
    boolean isActive,
    
    // Metadata
    String publisher,
    String isbn,
    Integer publicationYear,
    String language,
    Set<String> keywords,
    
    // Physical specs
    Integer pageCount,
    String coverType,
    Integer weight,
    Integer length,
    Integer width,
    Integer height,
    
    BigDecimal originalPrice,
    BigDecimal averageRating,
    int ratingCount,
    
    // Content segregation
    String tableOfContents,
    String excerpt,
    
    Set<Long> categoryIds
) {}
