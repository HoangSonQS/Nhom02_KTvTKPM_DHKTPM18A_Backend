package iuh.fit.se.modules.returns.adapter.inbound.web;

import iuh.fit.se.modules.returns.domain.ReturnReason;
import lombok.Data;

import java.util.List;

@Data
public class CreateReturnRequestDTO {
    private Long orderId;
    private ReturnReason reason;
    private String notes;
    private List<ReturnItemDTO> items;

    @Data
    public static class ReturnItemDTO {
        private Long bookId;
        private int quantity;
    }
}
