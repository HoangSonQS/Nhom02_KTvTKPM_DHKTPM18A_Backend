package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.EmbeddingSyncUseCase;
import iuh.fit.se.modules.ai.application.port.out.AiProcessedEventPort;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookDocument;
import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import iuh.fit.se.modules.ai.domain.BookVectorMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingSyncService implements EmbeddingSyncUseCase {

    private final CatalogBookPort catalogBookPort;
    private final VectorStorePort vectorStorePort;
    private final SemanticDocumentFactory documentFactory;
    private final AiProcessedEventPort idempotencyPort;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private EmbeddingSyncUseCase self;

    @Override
    @Async
    public void syncBook(Long bookId, UUID eventId) {
        log.info("Starting strict sync flow for bookId: {}, eventId: {}", bookId, eventId);

        try {
            BookDocument book = catalogBookPort.getBookDocument(bookId);
            if (book == null) {
                log.warn("Book not found for sync: {}", bookId);
                return;
            }

            // Step 1: Build Metadata & Compute Hash
            String richText = documentFactory.createWeightedText(book);
            String currentHash = documentFactory.calculateHash(richText);
            String currentVersion = documentFactory.getCurrentVersion();

            // Step 2: Check Existing Vector (Hash-First Skip)
            Optional<BookVectorMetadata> existing = vectorStorePort.getExistingMetadata(bookId);
            if (existing.isPresent() &&
                    currentHash.equals(existing.get().getContentHash()) &&
                    Integer.parseInt(currentVersion) == existing.get().getEmbeddingVersion()) {
                log.info("Vector for bookId {} is up to date (hash + version matched). Skipping AI call.", bookId);
                return;
            }

            // Step 3: Idempotency Lock (Status-aware)
            int lockResult = idempotencyPort.tryLockEvent(eventId);
            if (lockResult == 0) {
                if (idempotencyPort.isDone(eventId)) {
                    log.info("Event {} already processed successfully. Skipping.", eventId);
                } else {
                    log.warn("Event {} is currently in PROCESSING or crashed. Logic might retry or wait.", eventId);
                }
                return;
            }

            // Step 4: Execute LLM & Save Vector
            log.info("Calling LLM for bookId: {} (Hash changed or internal version update)", bookId);
            BookVectorMetadata metadata = BookVectorMetadata.builder()
                    .bookId(book.id())
                    .title(book.title())
                    .author(book.author())
                    .description(richText)
                    .category(book.categoryIds() != null
                            ? book.categoryIds().stream().map(String::valueOf).collect(Collectors.joining(", "))
                            : "General")
                    .contentHash(currentHash)
                    .embeddingVersion(Integer.parseInt(currentVersion))
                    .build();

            vectorStorePort.saveBookVector(metadata);

            // Step 5: Finalize Status
            idempotencyPort.markAsDone(eventId);
            log.info("Successfully synced bookId: {} and finalized event: {}", bookId, eventId);

        } catch (Exception e) {
            log.error("AI Sync failed for bookId: {}, eventId: {}", bookId, eventId, e);
            if (e.getMessage() != null && (e.getMessage().toLowerCase().contains("api key") || e.getMessage().toLowerCase().contains("unauthorized"))) {
                log.error("Possible INVALID API KEY. Please check GEMINI_API_KEY in .env.");
            }
        }
    }

    @Override
    @Async
    public void syncDeletion(Long bookId) {
        log.info("Syncing deletion for bookId: {}", bookId);
        try {
            vectorStorePort.deleteBookVector(bookId);
            log.info("Successfully removed vector for bookId: {}", bookId);
        } catch (Exception e) {
            log.error("Failed to delete vector for bookId: {}", bookId, e);
        }
    }

    @Override
    public void syncAllBooks() {
        log.info("Bulk sync triggered. Note: Simple loop, no individual idempotency IDs provided here.");
        List<BookDocument> books = catalogBookPort.searchBooks(null, null);
        if (books != null) {
            for (BookDocument book : books) {
                self.syncBook(book.id(), UUID.randomUUID());
            }
        }
    }
}
