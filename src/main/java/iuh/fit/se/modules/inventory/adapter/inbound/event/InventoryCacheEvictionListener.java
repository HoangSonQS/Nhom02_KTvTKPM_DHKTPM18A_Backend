package iuh.fit.se.modules.inventory.adapter.inbound.event;

import iuh.fit.se.modules.inventory.domain.InventoryStockDecreasedEvent;
import iuh.fit.se.modules.inventory.domain.InventoryStockIncreasedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryCacheEvictionListener {

    /**
     * 🔥 Xóa cache Redis ở Phase AFTER_COMMIT để phòng ngừa Cache Stampede (Race condition).
     * Chỉ thi hành nếu transaction DB commit trừ hàng thành công.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @CacheEvict(value = "inventory_stock_cache", key = "#event.bookId")
    public void onStockDecreased(InventoryStockDecreasedEvent event) {
        log.info("🔥 Evicted cache inventory_stock_cache for book {}. ID: {}. Remaining: {}", 
                event.getBookId(), event.getEventId(), event.getRemainingQuantity());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @CacheEvict(value = "inventory_stock_cache", key = "#event.bookId")
    public void onStockIncreased(InventoryStockIncreasedEvent event) {
        log.info("🔥 Evicted cache inventory_stock_cache (INCREASE) for book {}. ID: {}. New Total: {}", 
                event.getBookId(), event.getEventId(), event.getRemainingQuantity());
    }
}
