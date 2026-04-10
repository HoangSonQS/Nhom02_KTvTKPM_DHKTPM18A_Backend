package iuh.fit.se.modules.ai.adapter.inbound.event;

import iuh.fit.se.modules.ai.application.port.in.EmbeddingSyncUseCase;
import iuh.fit.se.shared.event.catalog.BookCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
        log.info("Received BookCreatedEvent for bookId: {}. Triggering AI embedding sync.", event.getBookId());
        syncUseCase.syncBook(event.getBookId());
    }
}
