package iuh.fit.se.modules.order.application.service.scheduler;

import iuh.fit.se.modules.order.application.port.out.InventoryPort;
import iuh.fit.se.modules.order.application.port.out.OrderPersistencePort;
import iuh.fit.se.modules.order.application.port.out.PromotionPort;
import iuh.fit.se.modules.order.domain.Order;
import iuh.fit.se.modules.order.domain.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupJob {

    private final OrderPersistencePort orderPersistencePort;
    private final InventoryPort inventoryPort;
    private final PromotionPort promotionPort;

    @Scheduled(fixedDelay = 5 * 60 * 1000) // Every 5 minutes
    public void cleanupGhostReservations() {
        MDC.put("requestId", "CLEANUP-" + System.currentTimeMillis());
        try {
            log.info("Executing Order Cleanup Job to reconcile ghost reservations...");
            
            // Find orders stuck in INIT, STOCK_RESERVED, or COUPON_RESERVED for > 15 mins
            LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(15);
            List<Order> candidates = orderPersistencePort.findAbandonedOrders(SagaStatus.COMPLETED, expirationTime, 100);

            if (candidates.isEmpty()) {
                return;
            }

            int compensatedCount = 0;
            for (Order order : candidates) {
                // Skip if already FAILED or COMPENSATED (though the query should exclude them if optimized)
                if (order.getSagaStatus() == SagaStatus.FAILED || order.getSagaStatus() == SagaStatus.COMPENSATED) {
                    continue;
                }

                if (processCompensation(order)) {
                    compensatedCount++;
                }
            }

            if (compensatedCount > 0) {
                log.info("Cleanup Job completed. Successfully compensated {} abandoned orders.", compensatedCount);
            }
            
            if (compensatedCount > 5) {
                log.warn("CRITICAL: High number of abandoned orders detected ({}). Please investigate system health.", compensatedCount);
            }

        } catch (Exception e) {
            log.error("Error during Order Cleanup Job execution: {}", e.getMessage(), e);
        } finally {
            MDC.remove("requestId");
        }
    }

    private boolean processCompensation(Order order) {
        SagaStatus originalStatus = order.getSagaStatus();
        
        // Atomic CAS to avoid double compensation
        boolean locked = orderPersistencePort.updateSagaStatusAtomic(order.getId(), originalStatus, SagaStatus.COMPENSATING);
        if (!locked) {
            return false;
        }

        try {
            log.info("Compensating Order {} (Stuck at: {})", order.getId(), originalStatus);

            // 1. Release Promotion if reserved
            if (originalStatus == SagaStatus.COUPON_RESERVED) {
                promotionPort.releaseCoupon(order.getRequestId());
            }

            // 2. Release Stock if reserved (at either STOCK_RESERVED or COUPON_RESERVED phase)
            if (originalStatus == SagaStatus.STOCK_RESERVED || originalStatus == SagaStatus.COUPON_RESERVED) {
                List<InventoryPort.StockItem> items = order.getItems().stream()
                        .map(item -> InventoryPort.StockItem.builder()
                                .bookId(item.getBookId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList());
                
                inventoryPort.increaseStockBulk(items, order.getRequestId());
            }

            // 3. Mark as COMPENSATED
            Order o = orderPersistencePort.findById(order.getId()).orElseThrow();
            o.markCompensated();
            orderPersistencePort.save(o);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to compensate order {}: {}. Will retry in next run.", order.getId(), e.getMessage());
            // It stays in COMPENSATING state, next run will pick it up or we can mark FAILED
            return false;
        }
    }
}
