package iuh.fit.se.modules.logistics.application.port.in;

import iuh.fit.se.modules.logistics.domain.PurchaseOrder;
import iuh.fit.se.modules.logistics.domain.Supplier;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public interface LogisticsUseCase {

    // --- Supplier Operations ---
    Supplier createSupplier(CreateSupplierCommand command);
    List<Supplier> getAllSuppliers();
    void deleteSupplier(Long id);

    // --- Purchase Order Operations ---
    PurchaseOrder createPurchaseOrder(CreatePOCommand command, String createdBy);
    PurchaseOrder submitPurchaseOrder(Long poId, String userRole);
    PurchaseOrder approvePurchaseOrder(Long poId, String userRole, String adminName);
    PurchaseOrder returnPurchaseOrder(Long poId, String userRole, String reason);
    PurchaseOrder receivePurchaseOrder(Long poId, String userRole, String receiverName);
    PurchaseOrder cancelPurchaseOrder(Long poId, String userRole, String userName, String reason);
    List<PurchaseOrder> getAllPurchaseOrders();
    PurchaseOrder getPurchaseOrderById(Long id);

    // --- Inventory Adjustment (Manual) ---
    void confirmStockAdjustment(StockAdjustmentCommand command, String userName);

    @Data
    @Builder
    class CreateSupplierCommand {
        private String name;
        private String contactPerson;
        private String phoneNumber;
        private String email;
        private String address;
        private String taxCode;
    }

    @Data
    @Builder
    class CreatePOCommand {
        private Long supplierId;
        private String note;
        private List<POItemCommand> items;
    }

    @Data
    @Builder
    class POItemCommand {
        private Long bookId;
        private Integer quantity;
        private BigDecimal price;
    }

    @Data
    @Builder
    class StockAdjustmentCommand {
        private Long bookId;
        private Integer adjustmentQuantity; // Có thể âm hoặc dương
        private String reason;
    }
}
