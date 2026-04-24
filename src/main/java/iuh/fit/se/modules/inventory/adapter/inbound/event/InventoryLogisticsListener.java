package iuh.fit.se.modules.inventory.adapter.inbound.event;

import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.shared.event.logistics.StockAdjustmentIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryLogisticsListener {

    private final InventoryPersistencePort inventoryPort;

    @EventListener
    @Transactional
    public void handleStockAdjustment(StockAdjustmentIntegrationEvent event) {
        log.info("Received StockAdjustmentIntegrationEvent: {}", event.getEventId());

        // 1. Kiểm tra Idempotency (Đã xử lý chưa?)
        if (inventoryPort.existsProcessedEvent(event.getEventId())) {
            log.warn("Event {} already processed, skipping.", event.getEventId());
            return;
        }

        // 2. Cập nhật kho nguyên tử
        inventoryPort.updateStockAtomic(event.getBookId(), event.getAdjustmentQuantity());

        // 3. Đánh dấu đã xử lý thành công
        inventoryPort.saveProcessedEvent(event.getEventId());

        log.info("Successfully adjusted stock for bookId: {} by quantity: {}", 
                event.getBookId(), event.getAdjustmentQuantity());
    }
}
