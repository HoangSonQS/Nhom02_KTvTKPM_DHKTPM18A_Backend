package iuh.fit.se.modules.logistics.adapter.inbound.web.dto;

import iuh.fit.se.modules.logistics.domain.PurchaseOrder;
import iuh.fit.se.modules.logistics.domain.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        SupplierResponse supplier,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        String createdBy,
        String approvedBy,
        String receivedBy,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime receivedAt,
        String cancelReason,
        String cancelledBy,
        LocalDateTime cancelledAt,
        Long version,
        String note,
        List<PurchaseOrderItemResponse> items
) {
    public static PurchaseOrderResponse from(PurchaseOrder po) {
        if (po == null) return null;
        return new PurchaseOrderResponse(
                po.getId(),
                SupplierResponse.from(po.getSupplier()),
                po.getStatus(),
                po.getTotalAmount(),
                po.getCreatedBy(),
                po.getApprovedBy(),
                po.getReceivedBy(),
                po.getCreatedAt(),
                po.getApprovedAt(),
                po.getReceivedAt(),
                po.getCancelReason(),
                po.getCancelledBy(),
                po.getCancelledAt(),
                po.getVersion(),
                po.getNote(),
                po.getItems() == null ? List.of() :
                        po.getItems().stream().map(PurchaseOrderItemResponse::from).toList()
        );
    }
}
