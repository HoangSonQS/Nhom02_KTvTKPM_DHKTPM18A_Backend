package iuh.fit.se.modules.ai.adapter.outbound.internal;

import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort;
import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InternalCatalogBookAdapter implements CatalogBookPort {

    private final BookUseCase bookUseCase;

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
                .averageRating(book.averageRating())
                .ratingCount(book.ratingCount())
                .build();
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
}
