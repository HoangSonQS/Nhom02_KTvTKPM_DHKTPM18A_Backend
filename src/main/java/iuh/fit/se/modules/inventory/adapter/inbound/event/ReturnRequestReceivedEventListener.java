package iuh.fit.se.modules.inventory.adapter.inbound.event;

import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.returns.domain.ItemCondition;
import iuh.fit.se.modules.returns.domain.ReturnRequestReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("inventoryReturnRequestReceivedEventListener")
@RequiredArgsConstructor
@Slf4j
public class ReturnRequestReceivedEventListener {

    private final InventoryPersistencePort inventoryPort;

    @EventListener
    @Transactional
    public void onReturnRequestReceived(ReturnRequestReceivedEvent event) {
        log.info("Received ReturnRequestReceivedEvent for returnId: {}", event.getReturnRequestId());

        // 1. Idempotency check: eventId could be returnRequestId or eventId
        // In the outbox pattern, it's safer to use the unique eventId
        if (inventoryPort.existsProcessedEvent(event.getEventId())) {
            log.warn("Return event {} already processed by inventory, skipping.", event.getEventId());
            return;
        }

        // 2. Process each item
        for (ReturnRequestReceivedEvent.ReturnedItemCondition item : event.getItems()) {
            if (item.getCondition() == ItemCondition.GOOD) {
                log.info("Restocking GOOD item: bookId={}, quantity={}", item.getBookId(), item.getQuantity());
                inventoryPort.updateStockAtomic(item.getBookId(), item.getQuantity());
            } else {
                log.warn("Item condition is DEFECTIVE for bookId={}, skipping restock.", item.getBookId());
            }
        }

        // 3. Mark as processed
        inventoryPort.saveProcessedEvent(event.getEventId());
        log.info("Successfully processed items for returnId: {}", event.getReturnRequestId());
    }
}
