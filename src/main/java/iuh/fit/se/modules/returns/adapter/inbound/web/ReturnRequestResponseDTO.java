package iuh.fit.se.modules.returns.adapter.inbound.web;

import iuh.fit.se.shared.event.returns.ItemCondition;
import iuh.fit.se.modules.returns.domain.ReturnStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReturnRequestResponseDTO {
    private String id;
    private Long orderId;
    private Long customerId;
    private ReturnStatus status;
    private BigDecimal refundAmount;
    private String reason;
    private String notes;
    private String evidenceImageUrl;
    private LocalDateTime createdAt;
    private List<ReturnItemResponseDTO> items;
    private List<ReturnHistoryResponseDTO> histories;

    @Data
    @Builder
    public static class ReturnItemResponseDTO {
        private String id;
        private Long bookId;
        private int quantity;
        private BigDecimal refundPrice;
        private ItemCondition condition;
    }

    @Data
    @Builder
    public static class ReturnHistoryResponseDTO {
        private String id;
        private ReturnStatus fromStatus;
        private ReturnStatus toStatus;
        private String changedBy;
        private LocalDateTime changedAt;
        private String note;
    }
}
