package iuh.fit.se.shared.event.returns;

import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
    ) {
        public static ReturnRequestReceivedIntegrationEvent of(String returnRequestId, Long orderId, List<ReturnedItemCondition> items, String correlationId) {
            return new ReturnRequestReceivedIntegrationEvent(
                    UUID.randomUUID().toString(),
                    correlationId,
                    returnRequestId,
                    orderId,
                    items
            );
        }
    }

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
