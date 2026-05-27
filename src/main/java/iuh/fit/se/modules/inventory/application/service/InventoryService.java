package iuh.fit.se.modules.inventory.application.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase.StockItemRequest;
import iuh.fit.se.modules.inventory.application.port.in.StockResult;
import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.modules.inventory.domain.InventoryStockDecreasedEvent;
import iuh.fit.se.modules.inventory.domain.InventoryStockIncreasedEvent;
import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.exception.PendingIdempotencyException;
import iuh.fit.se.shared.event.inventory.InventoryStockChangedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class InventoryService implements InventoryInternalUseCase {

    private final InventoryPersistencePort persistencePort;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationContext applicationContext;

    private InventoryService getSelf() {
        return applicationContext.getBean(InventoryService.class);
    }
    private final Random random = new Random();

    private static final int MAX_RETRY = 3;

    @Override
    public Map<Long, Integer> getAvailableStocks(List<Long> bookIds) {
        return persistencePort.findStocksByBookIds(bookIds).stream()
                .collect(Collectors.toMap(
                        InventoryStock::getBookId,
                        InventoryStock::getQuantity
                ));
    }

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
        return getSelf().processOperationWithRetry(bookId, amount, referenceId, true);
    }

    @Override
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "decreaseStockFallback")
    public StockResult decreaseStock(Long bookId, int amount, String referenceId) {
        return getSelf().processOperationWithRetry(bookId, amount, referenceId, false);
    }

    @Override
    @Transactional
    public List<StockResult> decreaseStockBulk(List<StockItemRequest> requests, String referenceId) {
        log.info("Saga decreaseStockBulk for ref: {}. Items: {}", referenceId, requests.size());
        List<StockResult> results = new ArrayList<>();
        for (StockItemRequest req : requests) {
            StockResult res = getSelf().executeTransactionalOperation(req.getBookId(), req.getAmount(), referenceId + "_" + req.getBookId(), "DECREASE");
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
    public List<StockResult> increaseStockBulk(List<StockItemRequest> requests, String referenceId) {
        log.info("Saga increaseStockBulk for ref: {}. Items: {}", referenceId, requests.size());
        List<StockResult> results = new ArrayList<>();
        for (StockItemRequest req : requests) {
            StockResult res = getSelf().executeTransactionalOperation(req.getBookId(), req.getAmount(), referenceId + "_" + req.getBookId(), "INCREASE");
            results.add(res);
        }
        return results;
    }

    private StockResult processOperationWithRetry(Long bookId, int amount, String referenceId, boolean isIncrease) {
        String type = isIncrease ? "INCREASE" : "DECREASE";
        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                return getSelf().executeTransactionalOperation(bookId, amount, referenceId, type);
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

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 3)
    @Caching(evict = {
            @CacheEvict(value = "inventory_stock_cache", key = "#bookId"),
            @CacheEvict(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #bookId)"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public StockResult executeTransactionalOperation(Long bookId, int amount, String baseReferenceId, String type) {
        String referenceId = baseReferenceId + "_" + type;
        boolean isIncrease = "INCREASE".equals(type);
        
        // 1. IDEMPOTENCY CHECK (Read first to avoid transaction failure on duplicate key)
        Optional<StockHistory> existingOpt = persistencePort.findHistoryByReferenceId(referenceId);
        if (existingOpt.isPresent()) {
            StockHistory existing = existingOpt.get();
            if (existing.getStatus() == StockHistoryStatus.SUCCESS) {
                return StockResult.builder()
                        .status(StockResult.Status.ALREADY_PROCESSED)
                        .bookId(bookId)
                        .message("Giao dịch đã được xử lý thành công trước đó.")
                        .build();
            }
            
            if (existing.getStatus() == StockHistoryStatus.PENDING) {
                if (existing.getLockedAt().isBefore(LocalDateTime.now().minusSeconds(5))) {
                    log.warn("Stale PENDING for ref {}. Overriding...", referenceId);
                    persistencePort.updateHistoryStatusAtomically(referenceId, StockHistoryStatus.PENDING, StockHistoryStatus.PENDING, LocalDateTime.now());
                } else {
                    throw new PendingIdempotencyException();
                }
            }
        } else {
            // 2. CREATE PENDING HISTORY
            try {
                StockHistory history = StockHistory.builder()
                        .referenceId(referenceId)
                        .bookId(bookId)
                        .amount(amount)
                        .type(type)
                        .status(StockHistoryStatus.PENDING)
                        .build();
                persistencePort.saveHistory(history);
            } catch (DataIntegrityViolationException e) {
                // Concurrent insertion - TX is tainted. Throw for retry.
                log.info("Concurrent idempotency record creation for ref: {}. Retrying...", referenceId);
                throw new ObjectOptimisticLockingFailureException(StockHistory.class, referenceId);
            }
        }

        // 3. LOAD STOCK FOR VALIDATION (NOT MODIFICATION)
        InventoryStock stock = persistencePort.findStockByBookId(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.INV_STOCK_NOT_FOUND));

        try {
            if (!isIncrease && stock.getQuantity() < amount) {
                throw new AppException(ErrorCode.INV_OUT_OF_STOCK);
            }
            
            // 4. ATOMIC UPDATE (Native SQL)
            int rowsAffected = isIncrease 
                    ? persistencePort.increaseStockAtomically(bookId, amount, stock.getVersion())
                    : persistencePort.decreaseStockAtomically(bookId, amount, stock.getVersion());
            
            if (rowsAffected == 0) {
                throw new ObjectOptimisticLockingFailureException(InventoryStock.class, bookId);
            }

            // 5. FINALIZE HISTORY
            int newQuantity = isIncrease ? stock.getQuantity() + amount : stock.getQuantity() - amount;
            
            StockHistory updatedHistory = persistencePort.findHistoryByReferenceId(referenceId)
                    .orElseThrow(() -> new IllegalStateException("History record disappeared: " + referenceId));
            updatedHistory.markSuccess("{\"remaining\":" + newQuantity + "}");
            persistencePort.saveHistory(updatedHistory);

            // 6. EVENT PUBLISHING
            if (isIncrease) {
                eventPublisher.publishEvent(InventoryStockIncreasedEvent.create(bookId, amount, newQuantity));
                eventPublisher.publishEvent(InventoryStockChangedIntegrationEvent.of(bookId, amount, newQuantity, "INCREASE"));
            } else {
                eventPublisher.publishEvent(InventoryStockDecreasedEvent.create(bookId, amount, newQuantity));
                eventPublisher.publishEvent(InventoryStockChangedIntegrationEvent.of(bookId, amount, newQuantity, "DECREASE"));
            }

            return StockResult.builder()
                    .status(StockResult.Status.SUCCESS)
                    .bookId(bookId)
                    .remainingQuantity(newQuantity)
                    .version(stock.getVersion() + 1)
                    .build();

        } catch (AppException e) {
            log.warn("Business failure in inventory operation for ref {}: {}", referenceId, e.getMessage());
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected failure in inventory operation for ref {}: ", referenceId, e);
            throw e;
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
