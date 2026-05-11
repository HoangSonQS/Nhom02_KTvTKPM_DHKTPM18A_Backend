package iuh.fit.se.modules.logistics.adapter.inbound.web.dto;

import iuh.fit.se.modules.logistics.domain.PurchaseOrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseOrderItemResponse(
        Long id,
        Long bookId,
        Integer quantity,
        BigDecimal priceAtOrder,
        String currency,
        LocalDateTime createdAt
) {
    public static PurchaseOrderItemResponse from(PurchaseOrderItem item) {
        if (item == null) return null;
        return new PurchaseOrderItemResponse(
                item.getId(),
                item.getBookId(),
                item.getQuantity(),
                item.getPriceAtOrder(),
                item.getCurrency(),
                item.getCreatedAt()
        );
    }
}
