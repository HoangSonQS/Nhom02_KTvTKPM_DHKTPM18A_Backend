package iuh.fit.se.modules.inventory.adapter.inbound.event;

import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
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
    private final InventoryInternalUseCase inventoryUseCase;

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

        // 2. Process each item through InventoryService so cache eviction and catalog sync always run.
        var restockItems = event.items().stream()
                .filter(item -> item.condition() == ItemCondition.GOOD)
                .map(item -> {
                    log.info("Restocking GOOD item: bookId={}, quantity={}", item.bookId(), item.quantity());
                    return InventoryInternalUseCase.StockItemRequest.builder()
                            .bookId(item.bookId())
                            .amount(item.quantity())
                            .build();
                })
                .toList();

        event.items().stream()
                .filter(item -> item.condition() != ItemCondition.GOOD)
                .forEach(item -> log.warn(
                        "Item condition is {} for bookId={}, skipping restock.",
                        item.condition(),
                        item.bookId()
                ));

        if (!restockItems.isEmpty()) {
            inventoryUseCase.increaseStockBulk(restockItems, "RET_RECEIVED_" + event.returnRequestId());
        }

        // 3. Mark as processed
        inventoryPort.saveProcessedEvent(java.util.UUID.fromString(event.id()));
        log.info("Successfully processed items for returnId: {}", event.returnRequestId());
    }
}
