package iuh.fit.se.modules.inventory.application.service;

import iuh.fit.se.modules.inventory.application.port.in.StocktakeUseCase;
import iuh.fit.se.modules.inventory.application.port.out.InventoryPersistencePort;
import iuh.fit.se.modules.inventory.application.port.out.StocktakePersistencePort;
import iuh.fit.se.modules.inventory.domain.InventoryStock;
import iuh.fit.se.modules.inventory.domain.StockHistory;
import iuh.fit.se.modules.inventory.domain.StocktakeItem;
import iuh.fit.se.modules.inventory.domain.StocktakeSession;
import iuh.fit.se.modules.inventory.domain.StocktakeStatus;
import iuh.fit.se.shared.event.inventory.InventoryStockChangedIntegrationEvent;
import iuh.fit.se.shared.event.realtime.AdminDataChangedRealtimeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StocktakeServiceTest {

    @Mock
    private StocktakePersistencePort stocktakePort;

    @Mock
    private InventoryPersistencePort inventoryPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void givenSelectedBooks_whenCreateStocktake_thenSnapshotCurrentSystemQuantity() {
        StocktakeService service = new StocktakeService(stocktakePort, inventoryPort, eventPublisher);
        when(inventoryPort.findStocksByBookIds(List.of(10L, 11L))).thenReturn(List.of(
                InventoryStock.builder().bookId(10L).quantity(7).version(1L).build(),
                InventoryStock.builder().bookId(11L).quantity(12).version(1L).build()
        ));
        when(stocktakePort.save(any(StocktakeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StocktakeUseCase.StocktakeResponse response = service.create(
                new StocktakeUseCase.CreateStocktakeCommand("Kiểm kê A", 2L, "warehouse@sebook.vn", List.of(10L, 11L)),
                "admin@sebook.vn"
        );

        assertThat(response.status()).isEqualTo(StocktakeStatus.IN_PROGRESS);
        assertThat(response.items())
                .extracting(StocktakeUseCase.StocktakeItemResponse::systemQuantity)
                .containsExactly(7, 12);
        assertThat(response.items())
                .extracting(StocktakeUseCase.StocktakeItemResponse::actualQuantity)
                .containsOnlyNulls();
        verify(inventoryPort, never()).setStockQuantity(any(), anyInt());
        verify(eventPublisher).publishEvent(any(AdminDataChangedRealtimeEvent.class));
    }

    @Test
    void givenInProgressSession_whenUpdateActualQuantities_thenCalculateDifferenceAndDoNotUpdateStock() {
        StocktakeService service = new StocktakeService(stocktakePort, inventoryPort, eventPublisher);
        StocktakeSession session = StocktakeSession.create(
                "Kiểm kê B",
                "admin@sebook.vn",
                2L,
                "warehouse@sebook.vn",
                List.of(StocktakeItem.snapshot(10L, 8))
        );
        ReflectionTestUtils.setField(session, "id", 99L);

        when(stocktakePort.findById(99L)).thenReturn(Optional.of(session));
        when(stocktakePort.save(any(StocktakeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StocktakeUseCase.StocktakeResponse response = service.updateActualQuantities(
                99L,
                new StocktakeUseCase.UpdateActualQuantitiesCommand(List.of(
                        new StocktakeUseCase.ActualQuantityItem(10L, 5, "Thiếu 3 cuốn")
                )),
                2L,
                "STAFF_WAREHOUSE"
        );

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.actualQuantity()).isEqualTo(5);
            assertThat(item.difference()).isEqualTo(-3);
            assertThat(item.note()).isEqualTo("Thiếu 3 cuốn");
        });
        verify(inventoryPort, never()).setStockQuantity(any(), anyInt());
        verify(inventoryPort, never()).saveHistory(any(StockHistory.class));
    }

    @Test
    void givenSubmittedSession_whenApprove_thenSetStockAndSaveStocktakeAdjustmentHistory() {
        StocktakeService service = new StocktakeService(stocktakePort, inventoryPort, eventPublisher);
        StocktakeSession session = StocktakeSession.create(
                "Kiểm kê C",
                "admin@sebook.vn",
                2L,
                "warehouse@sebook.vn",
                List.of(StocktakeItem.snapshot(10L, 8))
        );
        ReflectionTestUtils.setField(session, "id", 100L);
        session.getItems().get(0).recordActualQuantity(11, "Thừa 3 cuốn");
        session.submit("warehouse@sebook.vn");

        when(stocktakePort.findById(100L)).thenReturn(Optional.of(session));
        when(stocktakePort.save(any(StocktakeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryPort.findStockByBookId(10L)).thenReturn(Optional.of(
                InventoryStock.builder().bookId(10L).quantity(9).version(1L).build()
        ));
        when(inventoryPort.setStockQuantity(10L, 11)).thenReturn(1);

        StocktakeUseCase.StocktakeResponse response = service.approve(100L, "admin@sebook.vn");

        assertThat(response.status()).isEqualTo(StocktakeStatus.APPROVED);
        verify(inventoryPort).setStockQuantity(10L, 11);

        ArgumentCaptor<StockHistory> historyCaptor = ArgumentCaptor.forClass(StockHistory.class);
        verify(inventoryPort).saveHistory(historyCaptor.capture());
        StockHistory history = historyCaptor.getValue();
        assertThat(history.getReferenceId()).isEqualTo("STOCKTAKE_100_10");
        assertThat(history.getBookId()).isEqualTo(10L);
        assertThat(history.getAmount()).isEqualTo(2);
        assertThat(history.getType()).isEqualTo("STOCKTAKE_ADJUSTMENT");

        verify(eventPublisher).publishEvent(any(InventoryStockChangedIntegrationEvent.class));
        verify(eventPublisher).publishEvent(any(AdminDataChangedRealtimeEvent.class));
    }
}
