package iuh.fit.se.modules.inventory.adapter.outbound.persistence;

import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StockHistoryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryPersistenceAdapterTest {

    @Mock
    private InventoryJpaRepository inventoryJpaRepository;
    
    @Mock
    private StockHistoryJpaRepository stockHistoryJpaRepository;
    
    @Mock
    private JpaProcessedEventRepository processedEventRepository;

    @InjectMocks
    private InventoryPersistenceAdapter adapter;

    @Test
    @DisplayName("Tìm chứng khoán theo bookId")
    void findStockByBookIdTest() {
        Long bookId = 1L;
        InventoryStock stock = InventoryStock.builder().bookId(bookId).quantity(100).build();
        when(inventoryJpaRepository.findByBookId(bookId)).thenReturn(Optional.of(stock));

        Optional<InventoryStock> result = adapter.findStockByBookId(bookId);

        assertThat(result).isPresent();
        assertThat(result.get().getQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("Giảm kho atomic gọi đúng repository")
    void decreaseStockAtomicallyTest() {
        adapter.decreaseStockAtomically(1L, 10, 1L);
        verify(inventoryJpaRepository).decreaseQuantityAtomically(1L, 10, 1L);
    }

    @Test
    @DisplayName("Lưu lịch sử giao dịch")
    void saveHistoryTest() {
        StockHistory history = StockHistory.builder().referenceId("REF1").build();
        adapter.saveHistory(history);
        verify(stockHistoryJpaRepository).save(history);
    }

    @Test
    @DisplayName("Cập nhật trạng thái lịch sử atomic")
    void updateHistoryStatusAtomicallyTest() {
        LocalDateTime now = LocalDateTime.now();
        adapter.updateHistoryStatusAtomically("REF1", StockHistoryStatus.PENDING, StockHistoryStatus.SUCCESS, now);
        verify(stockHistoryJpaRepository).updateStatusAtomically("REF1", StockHistoryStatus.PENDING, StockHistoryStatus.SUCCESS, now);
    }
}
