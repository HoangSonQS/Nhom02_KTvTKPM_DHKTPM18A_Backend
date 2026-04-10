package iuh.fit.se.modules.returns.domain.event;

import iuh.fit.se.modules.returns.domain.ItemCondition;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

public class ReturnIntegrationEvents {

    public record ReturnRequestCreatedIntegrationEvent(
            String id,
            String correlationId,
            String returnRequestId,
            Long orderId,
            Long customerId
    ) {}

    public record ReturnRequestApprovedIntegrationEvent(
            String id,
            String correlationId,
            String returnRequestId,
            Long orderId
    ) {}

    public record ReturnRequestReceivedIntegrationEvent(
            String id,
            String correlationId,
            String returnRequestId,
            Long orderId,
            List<ReturnedItemCondition> items
    ) {}

    @Builder
    public record ReturnedItemCondition(
            Long bookId,
            Integer quantity,
            ItemCondition condition
    ) {}

    public record ReturnRequestRefundedIntegrationEvent(
            String id,
            String correlationId,
            String returnRequestId,
            Long orderId,
            BigDecimal refundAmount
    ) {}

    public record ReturnRequestRejectedIntegrationEvent(
            String id,
            String correlationId,
            String returnRequestId,
            Long orderId,
            String reason
    ) {}
}
