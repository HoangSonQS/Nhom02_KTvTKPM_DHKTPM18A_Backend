package iuh.fit.se.modules.inventory.application.service;

import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.inventory.application.port.in.StockResult;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.modules.inventory.domain.InventoryStockIncreasedEvent;
import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;
import iuh.fit.se.shared.event.inventory.InventoryStockChangedIntegrationEvent;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import iuh.fit.se.shared.exception.PendingIdempotencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryPersistencePort persistencePort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private InventoryService inventoryService;
 
    @BeforeEach
    void setUp() {
        // Manually create and spy
        InventoryService raw = new InventoryService(persistencePort, eventPublisher);
        inventoryService = spy(raw);
        raw.setSelf(inventoryService);
    }

    @Test
    @DisplayName("Tăng kho thành công - Lần đầu tiên")
    void increaseStock_Success() {
        Long bookId = 1L;
        int amount = 10;
        String refId = "REF001";
        String type = "INCREASE";
        String fullRef = refId + "_" + type;

        InventoryStock stock = InventoryStock.builder().bookId(bookId).quantity(50).version(1L).build();
        StockHistory pendingHistory = StockHistory.builder()
                .referenceId(fullRef)
                .status(StockHistoryStatus.PENDING)
                .lockedAt(LocalDateTime.now())
                .build();

        // Line 158 returns empty, Line 216 returns pending
        when(persistencePort.findHistoryByReferenceId(fullRef))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pendingHistory));
        
        when(persistencePort.findStockByBookId(bookId)).thenReturn(Optional.of(stock));
        when(persistencePort.increaseStockAtomically(eq(bookId), eq(amount), anyLong())).thenReturn(1);

        StockResult result = inventoryService.increaseStock(bookId, amount, refId);

        assertThat(result.getStatus()).isEqualTo(StockResult.Status.SUCCESS);
        assertThat(result.getRemainingQuantity()).isEqualTo(60);
        // 1 for PENDING, 1 for SUCCESS
        verify(persistencePort, times(2)).saveHistory(any(StockHistory.class));
        verify(eventPublisher).publishEvent(any(InventoryStockIncreasedEvent.class));
        verify(eventPublisher).publishEvent(any(InventoryStockChangedIntegrationEvent.class));
    }

    @Test
    @DisplayName("Giảm kho thành công - Idempotency xử lý lặp")
    void decreaseStock_AlreadyProcessed() {
        Long bookId = 1L;
        int amount = 5;
        String refId = "REF002";
        String fullRef = refId + "_DECREASE";

        StockHistory existingHistory = StockHistory.builder()
                .referenceId(fullRef)
                .status(StockHistoryStatus.SUCCESS)
                .build();

        when(persistencePort.findHistoryByReferenceId(fullRef)).thenReturn(Optional.of(existingHistory));

        StockResult result = inventoryService.decreaseStock(bookId, amount, refId);

        assertThat(result.getStatus()).isEqualTo(StockResult.Status.ALREADY_PROCESSED);
        verify(persistencePort, never()).decreaseStockAtomically(anyLong(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("Giảm kho thất bại - Hết hàng")
    void decreaseStock_OutOfStock() {
        Long bookId = 1L;
        int amount = 100;
        String refId = "REF003";
        String fullRef = refId + "_DECREASE";

        InventoryStock stock = InventoryStock.builder().bookId(bookId).quantity(50).version(1L).build();

        when(persistencePort.findHistoryByReferenceId(fullRef)).thenReturn(Optional.empty());
        when(persistencePort.findStockByBookId(bookId)).thenReturn(Optional.of(stock));

        StockResult result = inventoryService.decreaseStock(bookId, amount, refId);

        assertThat(result.getStatus()).isEqualTo(StockResult.Status.OUT_OF_STOCK);
        // Only 1 for PENDING
        verify(persistencePort, times(1)).saveHistory(any(StockHistory.class));
        verify(persistencePort, never()).decreaseStockAtomically(anyLong(), anyInt(), anyLong());
    }
    
    @Test
    @DisplayName("Xử lý tranh chấp Optimistic Lock - Retry thành công")
    void operation_OptimisticLockRetry() {
        Long bookId = 1L;
        int amount = 10;
        String refId = "REF004";
        String fullRef = refId + "_INCREASE";

        InventoryStock stock = InventoryStock.builder().bookId(bookId).quantity(50).version(1L).build();
        StockHistory pendingHistory = StockHistory.builder()
                .referenceId(fullRef)
                .status(StockHistoryStatus.PENDING)
                .lockedAt(LocalDateTime.now())
                .build();

        // Attempt 1: findHistory (empty), saveHistory, increase (fail -> throw retryable)
        // Attempt 2: findHistory (empty again due to rollback), saveHistory, increase (success), findHistory (pending -> for finalize)
        when(persistencePort.findHistoryByReferenceId(fullRef))
                .thenReturn(Optional.empty()) // Attempt 1 check
                .thenReturn(Optional.empty()) // Attempt 2 check
                .thenReturn(Optional.of(pendingHistory)); // Finalize check
                
        when(persistencePort.findStockByBookId(bookId)).thenReturn(Optional.of(stock));
        
        // Lần 1 fail, lần 2 success
        when(persistencePort.increaseStockAtomically(eq(bookId), eq(amount), anyLong()))
                .thenReturn(0)
                .thenReturn(1);

        StockResult result = inventoryService.increaseStock(bookId, amount, refId);

        assertThat(result.getStatus()).isEqualTo(StockResult.Status.SUCCESS);
        verify(persistencePort, times(2)).increaseStockAtomically(eq(bookId), eq(amount), anyLong());
        verify(persistencePort, times(3)).saveHistory(any(StockHistory.class));
    }


}
