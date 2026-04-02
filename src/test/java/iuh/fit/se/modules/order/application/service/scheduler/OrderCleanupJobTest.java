package iuh.fit.se.modules.order.application.service.scheduler;

import iuh.fit.se.modules.order.application.port.out.*;
import iuh.fit.se.modules.order.domain.Order;
import iuh.fit.se.modules.order.domain.OrderItem;
import iuh.fit.se.modules.order.domain.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCleanupJobTest {

        @Mock
        private OrderPersistencePort orderPersistencePort;
        @Mock
        private InventoryPort inventoryPort;
        @Mock
        private PromotionPort promotionPort;

        @InjectMocks
        private OrderCleanupJob orderCleanupJob;

        private Order stuckOrder;

        @BeforeEach
        void setUp() {
                stuckOrder = Order.builder()
                                .id(1L)
                                .userId(1L)
                                .totalAmount(new BigDecimal("100000"))
                                .requestId("req-stuck")
                                .sagaStatus(SagaStatus.STOCK_RESERVED)
                                .items(List.of(OrderItem.builder().bookId(101L).quantity(1).build()))
                                .build();
        }

        @Test
        void testCleanup_GhostReservations_Success() {
                // Given
                when(orderPersistencePort.findAbandonedOrders(eq(SagaStatus.COMPLETED), any(LocalDateTime.class),
                                eq(100)))
                                .thenReturn(List.of(stuckOrder));
                when(orderPersistencePort.updateSagaStatusAtomic(eq(1L), eq(SagaStatus.STOCK_RESERVED),
                                eq(SagaStatus.COMPENSATING)))
                                .thenReturn(true);
                when(orderPersistencePort.findById(1L)).thenReturn(Optional.of(stuckOrder));

                // When
                orderCleanupJob.cleanupGhostReservations();

                // Then
                verify(inventoryPort).increaseStockBulk(anyList(), eq("req-stuck"));
                verify(orderPersistencePort, atLeastOnce()).save(any(Order.class));
                verify(promotionPort, never()).releaseCoupon(anyString()); // Since status is STOCK_RESERVED, not
                                                                           // COUPON_RESERVED
        }

        @Test
        void testCleanup_AtomicLock_ShouldSkipIfAlreadyProcessing() {
                // Given
                when(orderPersistencePort.findAbandonedOrders(any(), any(), anyInt()))
                                .thenReturn(List.of(stuckOrder));
                when(orderPersistencePort.updateSagaStatusAtomic(anyLong(), any(), any()))
                                .thenReturn(false); // Simulated: another instance already locked it

                // When
                orderCleanupJob.cleanupGhostReservations();

                // Then
                verify(inventoryPort, never()).increaseStockBulk(anyList(), anyString());
        }
}
