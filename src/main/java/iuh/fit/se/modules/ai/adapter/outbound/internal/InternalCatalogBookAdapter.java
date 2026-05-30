package iuh.fit.se.modules.ai.adapter.outbound.internal;

import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort;
import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.CategoryDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.catalog.application.port.in.CategoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.text.Normalizer;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InternalCatalogBookAdapter implements CatalogBookPort {

    private final BookUseCase bookUseCase;
    private final CategoryUseCase categoryUseCase;

    @Override
    public BookContext getBook(Long bookId) {
        return mapToContext(bookUseCase.getBook(bookId));
    }

    @Override
    public BookDocument getBookDocument(Long bookId) {
        return mapToDocument(bookUseCase.getBook(bookId));
    }

    @Override
    public List<BookDocument> searchBooks(String title, Long categoryId) {
        return bookUseCase.searchBooks(title, categoryId).stream()
                .map(this::mapToDocument)
                .toList();
    }

    @Override
    public List<BookContext> searchBooksByCategoryName(String categoryName) {
        Long categoryId = categoryUseCase.getAllCategorySummaries().stream()
                .filter(CategoryDTO::active)
                .filter(category -> Objects.equals(normalize(category.name()), normalize(categoryName)))
                .map(CategoryDTO::id)
                .findFirst()
                .orElse(null);
        if (categoryId == null) {
            return List.of();
        }
        return bookUseCase.searchBooks(null, categoryId).stream()
                .map(this::mapToContext)
                .toList();
    }

    private BookContext mapToContext(BookDTO book) {
        return BookContext.builder()
                .id(book.id())
                .title(book.title())
                .author(book.author())
                .description(book.description())
                .price(book.price())
                .quantity(book.quantity())
                .isActive(book.isActive())
                .keywords(book.keywords())
                .categoryNames(resolveCategoryNames(book.categoryIds()))
                .averageRating(book.averageRating())
                .ratingCount(book.ratingCount())
                .build();
    }

    private Set<String> resolveCategoryNames(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Set.of();
        }
        Map<Long, String> namesById = categoryUseCase.getAllCategorySummaries().stream()
                .filter(CategoryDTO::active)
                .collect(Collectors.toMap(CategoryDTO::id, CategoryDTO::name, (first, ignored) -> first));
        return categoryIds.stream()
                .map(namesById::get)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());
    }

    private BookDocument mapToDocument(BookDTO book) {
        return BookDocument.builder()
                .id(book.id())
                .title(book.title())
                .author(book.author())
                .description(book.description())
                .keywords(book.keywords())
                .excerpt(book.excerpt())
                .publisher(book.publisher())
                .language(book.language())
                .categoryIds(book.categoryIds())
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
