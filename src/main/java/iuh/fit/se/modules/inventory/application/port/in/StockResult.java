package iuh.fit.se.modules.inventory.application.port.in;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockResult {
    private Status status;
    private Long bookId;
    private int requestedAmount;
    private int remainingQuantity;
    private Long version;
    private String message;

    public enum Status {
        SUCCESS,
        OUT_OF_STOCK,
        ALREADY_PROCESSED,
        RETRY_LATER,
        FAILED
    }
}
