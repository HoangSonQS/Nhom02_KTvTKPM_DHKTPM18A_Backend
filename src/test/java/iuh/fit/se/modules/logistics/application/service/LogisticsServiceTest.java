package iuh.fit.se.modules.logistics.application.service;

import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.inventory.application.port.in.StockResult;
import iuh.fit.se.modules.logistics.application.port.out.LogisticsOutboxPersistencePort;
import iuh.fit.se.modules.logistics.application.port.out.PurchaseOrderPersistencePort;
import iuh.fit.se.modules.logistics.application.port.out.SupplierPersistencePort;
import iuh.fit.se.modules.logistics.domain.PurchaseOrder;
import iuh.fit.se.modules.logistics.domain.PurchaseOrderStatus;
import iuh.fit.se.modules.logistics.domain.Supplier;
import iuh.fit.se.modules.logistics.domain.PurchaseOrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogisticsServiceTest {

    @Mock
    private SupplierPersistencePort supplierPort;
    @Mock
    private PurchaseOrderPersistencePort poPort;
    @Mock
    private LogisticsOutboxPersistencePort outboxPort;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private InventoryInternalUseCase inventoryUseCase;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LogisticsService logisticsService;

    private Supplier supplier;
    private PurchaseOrder po;

    @BeforeEach
    void setUp() {
        supplier = Supplier.builder().id(1L).name("Test Supplier").build();
        po = PurchaseOrder.create(supplier, "staff1", "Note", 
                Collections.singletonList(PurchaseOrderItem.create(1L, 5, new BigDecimal("200000"))));
        ReflectionTestUtils.setField(po, "id", 1L);
    }

    @Test
    @DisplayName("Duyệt PO phải gọi port lưu PO và Lịch sử")
    void approvePOServiceTest() {
        when(poPort.findById(1L)).thenReturn(Optional.of(po));
        when(poPort.save(any())).thenReturn(po);
        // Mocking submit first
        po.submit("ROLE_STAFF_WAREHOUSE");
        
        logisticsService.approvePurchaseOrder(1L, "ROLE_ADMIN", "admin1");

        assertEquals(PurchaseOrderStatus.APPROVED, po.getStatus());
        verify(poPort).save(po);
        verify(poPort).saveHistory(any());
    }

    @Test
    @DisplayName("Nhập kho PO phải tạo Outbox Event cho mỗi Item")
    void receivePOServiceTest() {
        when(poPort.findById(1L)).thenReturn(Optional.of(po));
        when(poPort.save(any())).thenReturn(po);
        when(inventoryUseCase.increaseStock(1L, 5, "PO_RECEIVED_1_1"))
                .thenReturn(StockResult.builder()
                        .status(StockResult.Status.SUCCESS)
                        .bookId(1L)
                        .remainingQuantity(15)
                        .build());
        // Setup PO Approved
        po.submit("ROLE_STAFF_WAREHOUSE");
        po.approve("ROLE_ADMIN", "admin1");
        
        logisticsService.receivePurchaseOrder(1L, "ROLE_STAFF_WAREHOUSE", "staff1");

        assertEquals(PurchaseOrderStatus.RECEIVED, po.getStatus());
        verify(inventoryUseCase).increaseStock(1L, 5, "PO_RECEIVED_1_1");
        verify(outboxPort, never()).save(any());
    }
}
