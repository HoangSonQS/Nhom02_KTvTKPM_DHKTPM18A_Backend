package iuh.fit.se.modules.inventory.adapter.inbound.event;

import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.shared.event.returns.ItemCondition;
import iuh.fit.se.shared.event.returns.ReturnIntegrationEvents.ReturnRequestReceivedIntegrationEvent;
import iuh.fit.se.shared.event.returns.ReturnIntegrationEvents.ReturnedItemCondition;
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
    public void onReturnRequestReceived(ReturnRequestReceivedIntegrationEvent event) {
        log.info("Received ReturnRequestReceivedEvent for returnId: {}", event.returnRequestId());

        // 1. Idempotency check: eventId could be returnRequestId or eventId
        // In the outbox pattern, it's safer to use the unique eventId
        if (inventoryPort.existsProcessedEvent(java.util.UUID.fromString(event.id()))) {
            log.warn("Return event {} already processed by inventory, skipping.", event.id());
            return;
        }

        // 2. Process each item
        for (ReturnedItemCondition item : event.items()) {
            if (item.condition() == ItemCondition.GOOD) {
                log.info("Restocking GOOD item: bookId={}, quantity={}", item.bookId(), item.quantity());
                inventoryPort.updateStockAtomic(item.bookId(), item.quantity());
            } else {
                log.warn("Item condition is DEFECTIVE for bookId={}, skipping restock.", item.bookId());
            }
        }

        // 3. Mark as processed
        inventoryPort.saveProcessedEvent(java.util.UUID.fromString(event.id()));
        log.info("Successfully processed items for returnId: {}", event.returnRequestId());
    }
}
