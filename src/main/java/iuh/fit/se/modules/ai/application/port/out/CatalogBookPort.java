package iuh.fit.se.modules.ai.application.port.out;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface CatalogBookPort {

    BookContext getBook(Long bookId);

    BookDocument getBookDocument(Long bookId);

    List<BookDocument> searchBooks(String title, Long categoryId);

    List<BookContext> searchBooksByCategoryName(String categoryName);

    @Builder
    record BookContext(
            Long id,
            String title,
            String author,
            String description,
            BigDecimal price,
            int quantity,
            boolean isActive,
            Set<String> keywords,
            Set<String> categoryNames,
            BigDecimal averageRating,
            int ratingCount
    ) {
    }

    @Builder
    record BookDocument(
            Long id,
            String title,
            String author,
            String description,
            Set<String> keywords,
            String excerpt,
            String publisher,
            String language,
            Set<Long> categoryIds
    ) {
    }
}
