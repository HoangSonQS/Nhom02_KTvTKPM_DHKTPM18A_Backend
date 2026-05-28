package iuh.fit.se.modules.inventory.adapter.inbound.event;

import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.inventory.application.port.in.StockResult;
import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.shared.event.logistics.StockAdjustmentIntegrationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryLogisticsListenerTest {

    @Mock
    private InventoryPersistencePort inventoryPort;

    @Mock
    private InventoryInternalUseCase inventoryUseCase;

    @InjectMocks
    private InventoryLogisticsListener listener;

    @Test
    @DisplayName("Xử lý event thành công nếu chưa được xử lý trước đó")
    void handleNewEventTest() {
        UUID eventId = UUID.randomUUID();
        StockAdjustmentIntegrationEvent event = StockAdjustmentIntegrationEvent.builder()
                .eventId(eventId)
                .bookId(1L)
                .adjustmentQuantity(10)
                .build();

        when(inventoryPort.existsProcessedEvent(eventId)).thenReturn(false);
        when(inventoryUseCase.increaseStock(eq(1L), eq(10), eq("LOG_STOCK_ADJUSTMENT_" + eventId)))
                .thenReturn(StockResult.builder()
                        .status(StockResult.Status.SUCCESS)
                        .bookId(1L)
                        .remainingQuantity(20)
                        .build());

        listener.handleStockAdjustment(event);

        verify(inventoryUseCase).increaseStock(1L, 10, "LOG_STOCK_ADJUSTMENT_" + eventId);
        verify(inventoryPort, never()).updateStockAtomic(anyLong(), anyInt());
        verify(inventoryPort).saveProcessedEvent(eventId);
    }

    @Test
    @DisplayName("Bỏ qua event nếu đã xử lý rồi (Idempotency)")
    void skipDuplicateEventTest() {
        UUID eventId = UUID.randomUUID();
        StockAdjustmentIntegrationEvent event = StockAdjustmentIntegrationEvent.builder()
                .eventId(eventId)
                .bookId(1L)
                .adjustmentQuantity(10)
                .build();

        when(inventoryPort.existsProcessedEvent(eventId)).thenReturn(true);

        listener.handleStockAdjustment(event);

        verify(inventoryPort, never()).updateStockAtomic(anyLong(), anyInt());
        verify(inventoryUseCase, never()).increaseStock(anyLong(), anyInt(), anyString());
        verify(inventoryPort, never()).saveProcessedEvent(any());
    }

    @Test
    @DisplayName("Không đánh dấu event đã xử lý nếu tăng tồn kho thất bại")
    void doNotMarkEventProcessedWhenIncreaseFailsTest() {
        UUID eventId = UUID.randomUUID();
        StockAdjustmentIntegrationEvent event = StockAdjustmentIntegrationEvent.builder()
                .eventId(eventId)
                .bookId(1L)
                .adjustmentQuantity(10)
                .build();

        when(inventoryPort.existsProcessedEvent(eventId)).thenReturn(false);
        when(inventoryUseCase.increaseStock(eq(1L), eq(10), anyString()))
                .thenReturn(StockResult.builder()
                        .status(StockResult.Status.FAILED)
                        .message("Stock not found")
                        .build());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> listener.handleStockAdjustment(event)
        );

        verify(inventoryPort, never()).saveProcessedEvent(any());
    }
}
