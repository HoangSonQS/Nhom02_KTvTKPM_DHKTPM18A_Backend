package iuh.fit.se.modules.ai.adapter.inbound.event;

import iuh.fit.se.modules.ai.application.port.in.EmbeddingSyncUseCase;
import iuh.fit.se.shared.event.catalog.BookCreatedEvent;
import iuh.fit.se.shared.event.catalog.BookDeletedEvent;
import iuh.fit.se.shared.event.catalog.BookUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogBookSyncListener {

    private final EmbeddingSyncUseCase syncUseCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookCreated(BookCreatedEvent event) {
        log.info("Received BookCreatedEvent for bookId: {}, eventId: {}", event.getBookId(), event.getEventId());
        syncUseCase.syncBook(event.getBookId(), event.getEventId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookUpdated(BookUpdatedEvent event) {
        log.info("Received BookUpdatedEvent for bookId: {}, eventId: {}", event.getBookId(), event.getEventId());
        syncUseCase.syncBook(event.getBookId(), event.getEventId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookDeleted(BookDeletedEvent event) {
        log.info("Received BookDeletedEvent for bookId: {}", event.getBookId());
        syncUseCase.syncDeletion(event.getBookId());
    }
}
