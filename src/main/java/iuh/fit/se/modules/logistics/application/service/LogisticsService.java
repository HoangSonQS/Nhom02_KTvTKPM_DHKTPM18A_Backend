package iuh.fit.se.modules.logistics.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.logistics.application.port.in.LogisticsUseCase;
import iuh.fit.se.modules.logistics.application.port.out.LogisticsOutboxPersistencePort;
import iuh.fit.se.modules.logistics.application.port.out.PurchaseOrderPersistencePort;
import iuh.fit.se.modules.logistics.application.port.out.SupplierPersistencePort;
import iuh.fit.se.modules.logistics.domain.*;
import iuh.fit.se.shared.event.logistics.StockAdjustmentIntegrationEvent;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsService implements LogisticsUseCase {

    private final SupplierPersistencePort supplierPort;
    private final PurchaseOrderPersistencePort poPort;
    private final LogisticsOutboxPersistencePort outboxPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Supplier createSupplier(CreateSupplierCommand command) {
        Supplier supplier = Supplier.create(
                command.getName(),
                command.getContactPerson(),
                command.getPhoneNumber(),
                command.getEmail(),
                command.getAddress(),
                command.getTaxCode()
        );
        return supplierPort.save(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierPort.findAllActive();
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierPort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_SUPPLIER_NOT_FOUND));
        supplier.softDelete();
        supplierPort.save(supplier);
    }

    @Override
    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePOCommand command, String createdBy) {
        Supplier supplier = supplierPort.findById(command.getSupplierId())
                .orElseThrow(() -> new AppException(ErrorCode.LOG_SUPPLIER_NOT_FOUND));

        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Đơn hàng phải có ít nhất một sản phẩm");
        }

        List<PurchaseOrderItem> items = command.getItems().stream()
                .map(item -> {
                    if (item.getPrice() == null) {
                        throw new AppException(ErrorCode.INVALID_INPUT,
                                "Giá sản phẩm (bookId=" + item.getBookId() + ") không được để trống");
                    }
                    if (item.getQuantity() == null || item.getQuantity() <= 0) {
                        throw new AppException(ErrorCode.INVALID_INPUT,
                                "Số lượng sản phẩm (bookId=" + item.getBookId() + ") phải lớn hơn 0");
                    }
                    return PurchaseOrderItem.create(item.getBookId(), item.getQuantity(), item.getPrice());
                })
                .collect(Collectors.toList());

        PurchaseOrder po = PurchaseOrder.create(
                supplier,
                createdBy,
                command.getNote(),
                items
        );

        PurchaseOrder saved = poPort.save(po);
        recordHistory(saved, null, PurchaseOrderStatus.DRAFT, saved.getCreatedBy(), "Tạo mới đơn mua hàng");
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder submitPurchaseOrder(Long poId, String userRole) {
        PurchaseOrder po = poPort.findById(poId)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_PO_NOT_FOUND));
        
        PurchaseOrderStatus oldStatus = po.getStatus();
        po.submit(userRole);
        
        PurchaseOrder saved = poPort.save(po);
        recordHistory(saved, oldStatus, PurchaseOrderStatus.SUBMITTED, saved.getCreatedBy(), "Gửi duyệt đơn mua hàng");
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder approvePurchaseOrder(Long poId, String userRole, String adminName) {
        PurchaseOrder po = poPort.findById(poId)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_PO_NOT_FOUND));

        PurchaseOrderStatus oldStatus = po.getStatus();
        po.approve(userRole, adminName);

        PurchaseOrder saved = poPort.save(po);
        recordHistory(saved, oldStatus, PurchaseOrderStatus.APPROVED, adminName, "Phê duyệt đơn mua hàng");
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder returnPurchaseOrder(Long poId, String userRole, String reason) {
        PurchaseOrder po = poPort.findById(poId)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_PO_NOT_FOUND));

        PurchaseOrderStatus oldStatus = po.getStatus();
        po.returnToDraft(userRole, reason);

        PurchaseOrder saved = poPort.save(po);
        recordHistory(saved, oldStatus, PurchaseOrderStatus.DRAFT, userRole, "Admin trả về để sửa: " + reason);
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder receivePurchaseOrder(Long poId, String userRole, String receiverName) {
        PurchaseOrder po = poPort.findById(poId)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_PO_NOT_FOUND));

        PurchaseOrderStatus oldStatus = po.getStatus();
        po.receive(userRole, receiverName);

        // Sau khi đã RECEIVED thì tự động tạo Event để cập nhật kho cho từng Item
        for (PurchaseOrderItem item : po.getItems()) {
            createStockAdjustmentOutbox(item.getBookId(), item.getQuantity(), "Nhập kho từ PO #" + poId, receiverName);
        }

        PurchaseOrder saved = poPort.save(po);
        recordHistory(saved, oldStatus, PurchaseOrderStatus.RECEIVED, receiverName, "Xác nhận nhập kho thành công");
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder cancelPurchaseOrder(Long poId, String userRole, String userName, String reason) {
        PurchaseOrder po = poPort.findById(poId)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_PO_NOT_FOUND));

        PurchaseOrderStatus oldStatus = po.getStatus();
        po.cancel(userRole, userName, reason);

        PurchaseOrder saved = poPort.save(po);
        recordHistory(saved, oldStatus, PurchaseOrderStatus.CANCELLED, userName, "Hủy đơn hàng: " + reason);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return poPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrderById(Long id) {
        return poPort.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_PO_NOT_FOUND));
    }

    @Override
    @Transactional
    public void confirmStockAdjustment(StockAdjustmentCommand command, String userName) {
        createStockAdjustmentOutbox(
                command.getBookId(),
                command.getAdjustmentQuantity(),
                command.getReason(),
                userName
        );
    }

    private void recordHistory(PurchaseOrder po, PurchaseOrderStatus from, PurchaseOrderStatus to, String by, String reason) {
        PurchaseOrderHistory history = PurchaseOrderHistory.record(po.getId(), from, to, by, reason);
        poPort.saveHistory(history);
    }

    private void createStockAdjustmentOutbox(Long bookId, Integer qty, String reason, String fromUser) {
        UUID eventId = UUID.randomUUID();
        StockAdjustmentIntegrationEvent event = StockAdjustmentIntegrationEvent.builder()
                .eventId(eventId)
                .bookId(bookId)
                .adjustmentQuantity(qty)
                .reason(reason)
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            LogisticsOutboxEvent outbox = LogisticsOutboxEvent.create("StockAdjustmentIntegrationEvent", payload);
            outbox.setId(eventId); // Sync eventId with outbox id for idempotency tracking
            outboxPort.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for bookId: {}", bookId, e);
            throw new RuntimeException("Lỗi xử lý dữ liệu event");
        }
    }
}
