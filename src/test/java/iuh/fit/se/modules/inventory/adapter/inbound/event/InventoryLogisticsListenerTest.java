package iuh.fit.se.modules.inventory.adapter.inbound.event;

import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.logistics.domain.event.StockAdjustmentConfirmedEvent;
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

    @InjectMocks
    private InventoryLogisticsListener listener;

    @Test
    @DisplayName("Xử lý event thành công nếu chưa được xử lý trước đó")
    void handleNewEventTest() {
        UUID eventId = UUID.randomUUID();
        StockAdjustmentConfirmedEvent event = StockAdjustmentConfirmedEvent.builder()
                .eventId(eventId)
                .bookId(1L)
                .adjustmentQuantity(10)
                .build();

        when(inventoryPort.existsProcessedEvent(eventId)).thenReturn(false);

        listener.handleStockAdjustment(event);

        verify(inventoryPort).updateStockAtomic(1L, 10);
        verify(inventoryPort).saveProcessedEvent(eventId);
    }

    @Test
    @DisplayName("Bỏ qua event nếu đã xử lý rồi (Idempotency)")
    void skipDuplicateEventTest() {
        UUID eventId = UUID.randomUUID();
        StockAdjustmentConfirmedEvent event = StockAdjustmentConfirmedEvent.builder()
                .eventId(eventId)
                .bookId(1L)
                .adjustmentQuantity(10)
                .build();

        when(inventoryPort.existsProcessedEvent(eventId)).thenReturn(true);

        listener.handleStockAdjustment(event);

        verify(inventoryPort, never()).updateStockAtomic(anyLong(), anyInt());
        verify(inventoryPort, never()).saveProcessedEvent(any());
    }
}
