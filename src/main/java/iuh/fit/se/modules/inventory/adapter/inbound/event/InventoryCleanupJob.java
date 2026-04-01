package iuh.fit.se.modules.inventory.adapter.inbound.event;

import iuh.fit.se.modules.inventory.adapter.outbound.persistence.StockHistoryJpaRepository;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryCleanupJob {

    private final StockHistoryJpaRepository historyRepository;

    /**
     * 🔥 Quét các bản ghi PENDING bị treo > 30s và mark thành FAILED.
     * Chạy mỗi 1 phút một lần.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupStalePendingTransactions() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
        
        // Dùng native query hoặc JPA repo để xử lý batch
        // Để đơn giản và an toàn, ta dùng một custom query trong repo
        int updated = historyRepository.markStalePendingAsFailed(threshold);
        
        if (updated > 0) {
            log.info("InventoryCleanupJob: Đã khôi phục {} bản ghi PENDING bị treo.", updated);
        }
    }
}
