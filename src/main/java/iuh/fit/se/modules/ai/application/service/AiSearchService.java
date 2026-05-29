package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.AiSearchUseCase;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookContext;
import iuh.fit.se.modules.ai.application.port.out.SalesRankingPort;
import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiSearchService implements AiSearchUseCase {

    private final VectorStorePort vectorStorePort;
    private final CatalogBookPort catalogBookPort;
    private final SalesRankingPort salesRankingPort;

    @Override
    public List<Long> searchSemantic(String query, int topK) {
        int requestedCount = AiCatalogMatchSupport.resolveRequestedBookCount(query, topK);
        if (AiCatalogMatchSupport.hasRankingIntent(query)) {
            return salesRankingPort.getTopSellingBooks(requestedCount).stream()
                    .limit(requestedCount)
                    .map(BookContext::id)
                    .toList();
        }

        List<BookContext> books = vectorStorePort.findSimilarBooks(query, Math.max(topK, requestedCount)).stream()
                .map(this::findBookSafely)
                .filter(Objects::nonNull)
                .filter(BookContext::isActive)
                .toList();

        List<BookContext> matchedBooks = AiCatalogMatchSupport.keepOnlyExplicitTopicMatches(query, books);
        matchedBooks = AiCatalogMatchSupport.keepOnlyCatalogIntentKeywordMatches(query, matchedBooks);
        matchedBooks = supplementWithCatalogCategoryBooks(query, matchedBooks, requestedCount);

        return matchedBooks.stream()
                .limit(requestedCount)
                .map(BookContext::id)
                .toList();
    }

    private List<BookContext> supplementWithCatalogCategoryBooks(
            String query,
            List<BookContext> currentBooks,
            int requestedCount
    ) {
        if (currentBooks.size() >= requestedCount) {
            return currentBooks;
        }
        List<String> topics = AiCatalogMatchSupport.categoryTopicsIn(query);
        if (topics.isEmpty()) {
            return currentBooks;
        }

        Map<Long, BookContext> booksById = new LinkedHashMap<>();
        currentBooks.stream()
                .filter(book -> book.id() != null)
                .forEach(book -> booksById.put(book.id(), book));

        for (String topic : topics) {
            List<BookContext> categoryBooks = catalogBookPort.searchBooksByCategoryName(topic);
            if (categoryBooks == null || categoryBooks.isEmpty()) {
                continue;
            }
            categoryBooks.stream()
                    .filter(Objects::nonNull)
                    .filter(book -> book.id() != null)
                    .filter(BookContext::isActive)
                    .forEach(book -> booksById.putIfAbsent(book.id(), book));
            if (booksById.size() >= requestedCount) {
                break;
            }
        }

        return booksById.values().stream().toList();
    }

    private BookContext findBookSafely(Long bookId) {
        try {
            return catalogBookPort.getBook(bookId);
        } catch (Exception ignored) {
            return null;
        }
    }
}
