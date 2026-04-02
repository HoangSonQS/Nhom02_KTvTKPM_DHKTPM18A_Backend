package iuh.fit.se.modules.inventory.application.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.inventory.application.port.in.StockResult;
import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.modules.inventory.domain.InventoryStockDecreasedEvent;
import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class InventoryService implements InventoryInternalUseCase {

    private final InventoryPersistencePort persistencePort;
    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random();

    private static final int MAX_RETRY = 3;

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "inventory_stock_cache", key = "#bookId", unless = "#result.status.name() != 'SUCCESS'")
    public StockResult getAvailableStock(Long bookId) {
        return persistencePort.findStockByBookId(bookId)
                .map(stock -> StockResult.builder()
                        .status(StockResult.Status.SUCCESS)
                        .bookId(bookId)
                        .remainingQuantity(stock.getQuantity())
                        .version(stock.getVersion())
                        .build())
                .orElse(StockResult.builder()
                        .status(StockResult.Status.FAILED)
                        .message("Stock not found for book " + bookId)
                        .build());
    }

    @Override
    public StockResult increaseStock(Long bookId, int amount, String referenceId) {
        // Implementation for increase stock (simpler, follow similar pattern)
        // For brevity in phase 3 foundation, focusing on decreaseStock elite pattern
        return processOperationWithRetry(bookId, amount, referenceId, true);
    }

    @Override
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "decreaseStockFallback")
    public StockResult decreaseStock(Long bookId, int amount, String referenceId) {
        return processOperationWithRetry(bookId, amount, referenceId, false);
    }

    @Override
    @Transactional
    public java.util.List<StockResult> decreaseStockBulk(java.util.List<InventoryInternalUseCase.StockItemRequest> requests, String referenceId) {
        log.info("Saga decreaseStockBulk for ref: {}. Items: {}", referenceId, requests.size());
        java.util.List<StockResult> results = new java.util.ArrayList<>();
        for (InventoryInternalUseCase.StockItemRequest req : requests) {
            StockResult res = executeTransactionalOperation(req.getBookId(), req.getAmount(), referenceId + "_" + req.getBookId(), false);
            if (res.getStatus() != StockResult.Status.SUCCESS && res.getStatus() != StockResult.Status.ALREADY_PROCESSED) {
                log.error("Bulk decrease failed at book {}: {}", req.getBookId(), res.getMessage());
                throw new AppException(ErrorCode.INV_OUT_OF_STOCK, "Saga bulk decrease failed for book " + req.getBookId());
            }
            results.add(res);
        }
        return results;
    }

    @Override
    @Transactional
    public java.util.List<StockResult> increaseStockBulk(java.util.List<InventoryInternalUseCase.StockItemRequest> requests, String referenceId) {
        log.info("Saga increaseStockBulk for ref: {}. Items: {}", referenceId, requests.size());
        java.util.List<StockResult> results = new java.util.ArrayList<>();
        for (InventoryInternalUseCase.StockItemRequest req : requests) {
            StockResult res = executeTransactionalOperation(req.getBookId(), req.getAmount(), referenceId + "_" + req.getBookId(), true);
            results.add(res);
        }
        return results;
    }

    private StockResult processOperationWithRetry(Long bookId, int amount, String referenceId, boolean isIncrease) {
        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                return executeTransactionalOperation(bookId, amount, referenceId, isIncrease);
            } catch (PendingIdempotencyException e) {
                if (i == 1) {
                    log.info("PENDING fresh for ref {}. Short-polling (50ms)...", referenceId);
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    continue;
                }
                return StockResult.builder()
                        .status(StockResult.Status.RETRY_LATER)
                        .message(ErrorCode.INV_IDEMPOTENCY_PENDING.getMessage())
                        .build();
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Optimistic lock conflict for book {}. Retry {}/{}", bookId, i, MAX_RETRY);
                if (i < MAX_RETRY) {
                    long backoff = 100 + random.nextInt(50); // 100-150ms jitter
                    try { Thread.sleep(backoff); } catch (InterruptedException ignored) {}
                }
            } catch (AppException e) {
                return StockResult.builder()
                        .status(e.getErrorCode() == ErrorCode.INV_OUT_OF_STOCK ? StockResult.Status.OUT_OF_STOCK : StockResult.Status.FAILED)
                        .message(e.getMessage())
                        .build();
            } catch (Exception e) {
                log.error("Unexpected error in inventory operation: ", e);
                return StockResult.builder()
                        .status(StockResult.Status.FAILED)
                        .message(e.getMessage())
                        .build();
            }
        }
        return StockResult.builder()
                .status(StockResult.Status.RETRY_LATER)
                .message("Hệ thống bận dồn dập, vui lòng thử lại sau.")
                .build();
    }

    @Transactional(timeout = 3)
    public StockResult executeTransactionalOperation(Long bookId, int amount, String referenceId, boolean isIncrease) {
        // 1. UPSERT IDEMPOTENCY
        try {
            StockHistory history = StockHistory.builder()
                    .referenceId(referenceId)
                    .bookId(bookId)
                    .amount(amount)
                    .status(StockHistoryStatus.PENDING)
                    .build();
            persistencePort.saveHistory(history);
        } catch (DataIntegrityViolationException e) {
            // Already exists (UNIQUE(reference_id))
            StockHistory existing = persistencePort.findHistoryByReferenceId(referenceId)
                    .orElseThrow(() -> new IllegalStateException("History record disappeared!"));
            
            if (existing.getStatus() == StockHistoryStatus.SUCCESS) {
                return StockResult.builder()
                        .status(StockResult.Status.ALREADY_PROCESSED)
                        .bookId(bookId)
                        .message("Giao dịch đã được xử lý thành công trước đó.")
                        .build();
            }
            
            if (existing.getStatus() == StockHistoryStatus.PENDING) {
                // Check stale (5s) for eventual recovery if background job not run yet
                if (existing.getLockedAt().isBefore(LocalDateTime.now().minusSeconds(5))) {
                    log.warn("Stale PENDING for ref {}. Overriding...", referenceId);
                    int updated = persistencePort.updateHistoryStatusAtomically(referenceId, StockHistoryStatus.PENDING, StockHistoryStatus.PENDING, LocalDateTime.now());
                    if (updated == 0) {
                        log.warn("CAS retry failed for ref: {}. Someone else took it.", referenceId);
                    }
                } else {
                    throw new PendingIdempotencyException();
                }
            }
            // If FAILED, we let it fall through to restart the flow
        }

        // 2. LOAD STOCK AND PROCESS
        InventoryStock stock = persistencePort.findStockByBookId(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.INV_STOCK_NOT_FOUND));

        try {
            if (isIncrease) {
                stock.increase(amount);
            } else {
                stock.decrease(amount);
            }
            
            // 3. ATOMIC UPDATE (ROWS AFFECTED CHECK)
            int rowsAffected = isIncrease 
                    ? persistencePort.increaseStockAtomically(bookId, amount, stock.getVersion())
                    : persistencePort.decreaseStockAtomically(bookId, amount, stock.getVersion());
            
            if (rowsAffected == 0) {
                throw new ObjectOptimisticLockingFailureException(InventoryStock.class, bookId);
            }

            // 4. FINALIZE HISTORY
            StockHistory updatedHistory = persistencePort.findHistoryByReferenceId(referenceId).get();
            updatedHistory.markSuccess("{\"remaining\":" + stock.getQuantity() + "}");
            persistencePort.saveHistory(updatedHistory);

            // 5. EVENT PUBLISH (AFTER_COMMIT handles by TransactionalEventListener)
            if (!isIncrease) {
                eventPublisher.publishEvent(InventoryStockDecreasedEvent.create(bookId, amount, stock.getQuantity()));
            }

            return StockResult.builder()
                    .status(StockResult.Status.SUCCESS)
                    .bookId(bookId)
                    .remainingQuantity(stock.getQuantity())
                    .version(stock.getVersion() + 1)
                    .build();

        } catch (IllegalStateException e) {
            // Out of stock or invalid amount
            StockHistory updatedHistory = persistencePort.findHistoryByReferenceId(referenceId).get();
            updatedHistory.markFailed("{\"error\":\"" + e.getMessage() + "\"}");
            persistencePort.saveHistory(updatedHistory);
            throw new AppException(ErrorCode.INV_OUT_OF_STOCK);
        }
    }

    public StockResult decreaseStockFallback(Long bookId, int amount, String referenceId, Throwable t) {
        log.error("Circuit Breaker OPEN for Inventory. Ref: {}", referenceId);
        return StockResult.builder()
                .status(StockResult.Status.RETRY_LATER)
                .message("Dịch vụ kho đang tạm ngưng do quá tải. Thử lại sau 10s.")
                .build();
    }
}
