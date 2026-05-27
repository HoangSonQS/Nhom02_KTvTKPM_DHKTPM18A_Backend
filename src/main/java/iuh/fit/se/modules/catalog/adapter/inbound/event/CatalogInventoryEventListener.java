package iuh.fit.se.modules.catalog.adapter.inbound.event;

import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.inventory.domain.InventoryStockDecreasedEvent;
import iuh.fit.se.modules.inventory.domain.InventoryStockIncreasedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogInventoryEventListener {

    private final BookUseCase bookUseCase;

    /**
     * Đồng bộ dữ liệu Catalog sau khi Inventory thay đổi thành công.
     * Sync after the inventory transaction commits.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockDecreased(InventoryStockDecreasedEvent event) {
        log.info("📢 Catalog Sync: Trừ kho cho sách {} -> Còn lại: {}", event.getBookId(), event.getRemainingQuantity());
        bookUseCase.syncStock(event.getBookId(), event.getRemainingQuantity());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockIncreased(InventoryStockIncreasedEvent event) {
        log.info("📢 Catalog Sync: Cộng kho cho sách {} -> Còn lại: {}", event.getBookId(), event.getRemainingQuantity());
        bookUseCase.syncStock(event.getBookId(), event.getRemainingQuantity());
    }
}
