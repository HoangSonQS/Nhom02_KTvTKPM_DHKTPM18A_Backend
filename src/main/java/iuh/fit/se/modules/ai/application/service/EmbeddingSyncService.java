package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.EmbeddingSyncUseCase;
import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import iuh.fit.se.modules.ai.domain.BookVectorMetadata;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.catalog.domain.Book;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingSyncService implements EmbeddingSyncUseCase {

    private final BookUseCase bookUseCase;
    private final VectorStorePort vectorStorePort;

    @Override
    @Async
    public void syncBook(Long bookId) {
        log.info("Syncing book embedding for bookId: {}", bookId);
        try {
            Book book = bookUseCase.getBook(bookId);
            if (book != null) {
                BookVectorMetadata metadata = BookVectorMetadata.builder()
                        .bookId(book.getId())
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .description(book.getDescription())
                        // Category info might need a better way to join strings
                        .category(book.getCategoryIds() != null ? 
                                book.getCategoryIds().stream()
                                    .map(String::valueOf)
                                    .reduce((a, b) -> a + ", " + b)
                                    .orElse("General") : "General")
                        .build();
                
                vectorStorePort.saveBookVector(metadata);
                log.info("Successfully synced bookId: {}", bookId);
            }
        } catch (Exception e) {
            log.error("Failed to sync book embedding for bookId: {}", bookId, e);
        }
    }

    @Override
    public void syncAllBooks() {
        log.info("Starting bulk sync for all books...");
        // This might be expensive, in a real app we'd paginate
        List<Book> books = bookUseCase.searchBooks(null, null);
        if (books != null) {
            log.info("Found {} books to sync", books.size());
            for (Book book : books) {
                syncBook(book.getId());
            }
        }
    }
}
