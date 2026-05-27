package iuh.fit.se.modules.inventory.adapter.inbound.event;

import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.shared.event.catalog.BookCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryBookListener {

    private final InventoryPersistencePort persistencePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBookCreated(BookCreatedEvent event) {
        log.info("Initializing inventory for new book: {}. Initial Quantity: {}", 
                event.getBookId(), event.getInitialQuantity());
        
        // Ensure idempotency: check if stock already exists
        if (persistencePort.findStockByBookId(event.getBookId()).isEmpty()) {
            InventoryStock stock = InventoryStock.create(event.getBookId(), event.getInitialQuantity());
            persistencePort.saveStock(stock);
            log.info("Inventory initialized for book {}", event.getBookId());
        } else {
            log.warn("Inventory already exists for book {}, skipping initialization", event.getBookId());
        }
    }
}
