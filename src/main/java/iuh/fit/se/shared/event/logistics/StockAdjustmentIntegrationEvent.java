package iuh.fit.se.shared.event.logistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * StockAdjustmentIntegrationEvent — Sử dụng để đồng bộ kho giữa Logistics và Inventory.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockAdjustmentIntegrationEvent implements Serializable {
    @Builder.Default
    private final UUID eventId = UUID.randomUUID();
    
    private Long bookId;
    private Integer adjustmentQuantity;
    private String reason;
}
