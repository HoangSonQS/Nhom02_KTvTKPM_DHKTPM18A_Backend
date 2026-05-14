package iuh.fit.se.modules.inventory.application.service;

import iuh.fit.se.modules.inventory.application.port.in.InventoryAdminUseCase;
import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryAdminService implements InventoryAdminUseCase {

    private final InventoryPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllStocks() {
        return persistencePort.findAllStocks().stream()
                .map(InventoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getStockByBookId(Long bookId) {
        InventoryStock stock = persistencePort.findStockByBookId(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.INV_STOCK_NOT_FOUND));
        return InventoryResponse.from(stock);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_INIT_STOCK")
    @Caching(evict = {
            @CacheEvict(value = "inventory_stock_cache", key = "#bookId"),
            @CacheEvict(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #bookId)"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public InventoryResponse initializeStock(Long bookId, int initialQuantity) {
        if (persistencePort.findStockByBookId(bookId).isPresent()) {
            throw new AppException(ErrorCode.INV_STOCK_ALREADY_EXISTS);
        }

        InventoryStock stock = InventoryStock.create(bookId, initialQuantity);
        persistencePort.saveStock(stock);
        log.info("[ADMIN] Initialized stock for bookId={} with quantity={}", bookId, initialQuantity);
        return InventoryResponse.from(stock);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_INCREASE_STOCK")
    @Caching(evict = {
            @CacheEvict(value = "inventory_stock_cache", key = "#bookId"),
            @CacheEvict(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #bookId)"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public InventoryResponse increaseStock(Long bookId, int amount) {
        InventoryStock stock = persistencePort.findStockByBookId(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.INV_STOCK_NOT_FOUND));

        stock.increase(amount);
        persistencePort.saveStock(stock);
        log.info("[ADMIN] Increased stock for bookId={} by amount={}. New total={}", bookId, amount, stock.getQuantity());
        return InventoryResponse.from(stock);
    }

    @Override
    @Transactional
    @iuh.fit.se.shared.audit.annotation.Auditable(action = "STAFF_DECREASE_STOCK")
    @Caching(evict = {
            @CacheEvict(value = "inventory_stock_cache", key = "#bookId"),
            @CacheEvict(value = "bookDetails", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('bookDetails', #bookId)"),
            @CacheEvict(value = "books", allEntries = true)
    })
    public InventoryResponse decreaseStock(Long bookId, int amount) {
        InventoryStock stock = persistencePort.findStockByBookId(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.INV_STOCK_NOT_FOUND));

        try {
            stock.decrease(amount);
        } catch (IllegalStateException e) {
            throw new AppException(ErrorCode.INV_OUT_OF_STOCK);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INV_OUT_OF_STOCK); 
        }

        persistencePort.saveStock(stock);
        log.info("[ADMIN] Decreased stock for bookId={} by amount={}. New total={}", bookId, amount, stock.getQuantity());
        return InventoryResponse.from(stock);
    }
}
