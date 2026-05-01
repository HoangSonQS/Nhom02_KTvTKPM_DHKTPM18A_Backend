package iuh.fit.se.modules.inventory.application.service;

import iuh.fit.se.modules.inventory.application.port.in.InventoryAdminUseCase;
import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public List<InventoryStock> getAllStocks() {
        return persistencePort.findAllStocks();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStock getStockByBookId(Long bookId) {
        return persistencePort.findStockByBookId(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.INV_STOCK_NOT_FOUND));
    }

    @Override
    @Transactional
    public InventoryStock initializeStock(Long bookId, int initialQuantity) {
        if (persistencePort.findStockByBookId(bookId).isPresent()) {
            throw new AppException(ErrorCode.INV_STOCK_ALREADY_EXISTS);
        }

        InventoryStock stock = InventoryStock.create(bookId, initialQuantity);
        persistencePort.saveStock(stock);
        log.info("[ADMIN] Initialized stock for bookId={} with quantity={}", bookId, initialQuantity);
        return stock;
    }

    @Override
    @Transactional
    public InventoryStock increaseStock(Long bookId, int amount) {
        InventoryStock stock = persistencePort.findStockByBookId(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.INV_STOCK_NOT_FOUND));

        stock.increase(amount);
        persistencePort.saveStock(stock);
        log.info("[ADMIN] Increased stock for bookId={} by amount={}. New total={}", bookId, amount, stock.getQuantity());
        return stock;
    }

    @Override
    @Transactional
    public InventoryStock decreaseStock(Long bookId, int amount) {
        InventoryStock stock = persistencePort.findStockByBookId(bookId)
                .orElseThrow(() -> new AppException(ErrorCode.INV_STOCK_NOT_FOUND));

        try {
            stock.decrease(amount);
        } catch (IllegalStateException e) {
            throw new AppException(ErrorCode.INV_OUT_OF_STOCK);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INV_OUT_OF_STOCK); // or better error if amount < 0, but validation should catch this
        }

        persistencePort.saveStock(stock);
        log.info("[ADMIN] Decreased stock for bookId={} by amount={}. New total={}", bookId, amount, stock.getQuantity());
        return stock;
    }
}
