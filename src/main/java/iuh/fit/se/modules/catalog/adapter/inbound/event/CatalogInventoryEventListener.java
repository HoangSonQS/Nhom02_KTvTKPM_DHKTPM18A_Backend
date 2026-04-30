package iuh.fit.se.modules.catalog.adapter.inbound.event;

import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.inventory.domain.InventoryStockDecreasedEvent;
import iuh.fit.se.modules.inventory.domain.InventoryStockIncreasedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogInventoryEventListener {

    private final BookUseCase bookUseCase;

    /**
     * Đồng bộ dữ liệu Catalog sau khi Inventory thay đổi thành công.
     * Sử dụng @EventListener để đảm bảo sync ngay lập tức trong transaction của Inventory.
     */
    @EventListener
    public void handleStockDecreased(InventoryStockDecreasedEvent event) {
        log.info("📢 Catalog Sync: Trừ kho cho sách {} -> Còn lại: {}", event.getBookId(), event.getRemainingQuantity());
        bookUseCase.syncStock(event.getBookId(), event.getRemainingQuantity());
    }

    @EventListener
    public void handleStockIncreased(InventoryStockIncreasedEvent event) {
        log.info("📢 Catalog Sync: Cộng kho cho sách {} -> Còn lại: {}", event.getBookId(), event.getRemainingQuantity());
        bookUseCase.syncStock(event.getBookId(), event.getRemainingQuantity());
    }
}
