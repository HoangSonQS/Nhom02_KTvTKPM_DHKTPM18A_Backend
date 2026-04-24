package iuh.fit.se.modules.returns.application.port.in;

import iuh.fit.se.shared.event.returns.ItemCondition;
import iuh.fit.se.modules.returns.domain.ReturnReason;
import iuh.fit.se.modules.returns.domain.ReturnRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public interface ReturnRequestUseCase {

    ReturnRequest createReturn(CreateReturnCommand command);

    void approve(String returnRequestId, String approvedBy);

    void markAsReceived(String returnRequestId, String receivedBy, List<ItemCondition> conditions);

    void refund(String returnRequestId, String processedBy);

    void reject(String returnRequestId, String reason, String rejectedBy);

    ReturnRequest getById(String id);

    List<ReturnRequest> getAll();

    List<ReturnRequest> getByCustomer(Long customerId);

    @Getter
    @Builder
    class CreateReturnCommand {
        private Long orderId;
        private Long customerId;
        private ReturnReason reason;
        private String notes;
        private List<ReturnItemCommand> items;
    }

    @Getter
    @Builder
    class ReturnItemCommand {
        private Long bookId;
        private int quantity;
    }
}
