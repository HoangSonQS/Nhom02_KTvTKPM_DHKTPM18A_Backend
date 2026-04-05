package iuh.fit.se.modules.logistics.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentConfirmedEvent {
    private UUID eventId;
    private Long bookId;
    private Integer adjustmentQuantity;
    private String reason;
    private String confirmedBy;
}
