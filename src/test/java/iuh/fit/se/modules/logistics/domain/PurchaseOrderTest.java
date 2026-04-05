package iuh.fit.se.modules.logistics.domain;

import iuh.fit.se.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderTest {

    private PurchaseOrder po;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        supplier = Supplier.builder().id(1L).name("Test Supplier").build();
        List<PurchaseOrderItem> items = new ArrayList<>();
        items.add(PurchaseOrderItem.create(1L, 10, new BigDecimal("100000")));
        
        po = PurchaseOrder.create(supplier, "staff1", "Note test", items);
    }

    @Test
    @DisplayName("Tạo mới PO phải ở trạng thái DRAFT")
    void createPOTest() {
        assertEquals(PurchaseOrderStatus.DRAFT, po.getStatus());
        assertTrue(new BigDecimal("1000000.00").compareTo(po.getTotalAmount()) == 0);
    }

    @Test
    @DisplayName("Submit PO thành công từ DRAFT by STAFF_WAREHOUSE")
    void submitPOTest() {
        po.submit("ROLE_STAFF_WAREHOUSE");
        assertEquals(PurchaseOrderStatus.SUBMITTED, po.getStatus());
    }

    @Test
    @DisplayName("Approve PO thành công by ADMIN")
    void approvePOTest() {
        po.submit("ROLE_STAFF_WAREHOUSE");
        po.approve("ROLE_ADMIN", "admin1");
        assertEquals(PurchaseOrderStatus.APPROVED, po.getStatus());
    }

    @Test
    @DisplayName("Từ chối Approve nếu không phải ADMIN")
    void unauthorizedApproveTest() {
        po.submit("ROLE_STAFF_WAREHOUSE");
        assertThrows(AppException.class, () -> po.approve("ROLE_STAFF_WAREHOUSE", "staff1"));
    }

    @Test
    @DisplayName("Xác nhận nhập kho thành công")
    void receivePOTest() {
        po.submit("ROLE_STAFF_WAREHOUSE");
        po.approve("ROLE_ADMIN", "admin1");
        po.receive("ROLE_STAFF_WAREHOUSE", "staff1");
        assertEquals(PurchaseOrderStatus.RECEIVED, po.getStatus());
    }

    @Test
    @DisplayName("Hủy PO đã APPROVED phải qua ADMIN")
    void cancelApprovedPOTest() {
        po.submit("ROLE_STAFF_WAREHOUSE");
        po.approve("ROLE_ADMIN", "admin1");
        
        // Staff warehouse không được hủy khi đã Approved
        assertThrows(AppException.class, () -> po.cancel("ROLE_STAFF_WAREHOUSE", "staff1", "Reason"));
        
        // Admin được phép
        po.cancel("ROLE_ADMIN", "admin1", "Reason");
        assertEquals(PurchaseOrderStatus.CANCELLED, po.getStatus());
    }
}
