package iuh.fit.se.modules.returns.domain.event;

import iuh.fit.se.modules.returns.domain.ReturnRequest;
import iuh.fit.se.shared.domain.BaseEvent;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

public class ReturnDomainEvents {

    @Getter
    @SuperBuilder
    public static class ReturnRequestCreatedDomainEvent extends BaseEvent {
        private final ReturnRequest returnRequest;

        public static ReturnRequestCreatedDomainEvent of(ReturnRequest request) {
            return ReturnRequestCreatedDomainEvent.builder()
                    .correlationId("RET-" + request.getId())
                    .eventType("RETURN_CREATED_DOMAIN")
                    .returnRequest(request)
                    .build();
        }
    }

    @Getter
    @SuperBuilder
    public static class ReturnRequestApprovedDomainEvent extends BaseEvent {
        private final ReturnRequest returnRequest;

        public static ReturnRequestApprovedDomainEvent of(ReturnRequest request) {
            return ReturnRequestApprovedDomainEvent.builder()
                    .correlationId("RET-" + request.getId())
                    .eventType("RETURN_APPROVED_DOMAIN")
                    .returnRequest(request)
                    .build();
        }
    }

    @Getter
    @SuperBuilder
    public static class ReturnRequestReceivedDomainEvent extends BaseEvent {
        private final ReturnRequest returnRequest;

        public static ReturnRequestReceivedDomainEvent of(ReturnRequest request) {
            return ReturnRequestReceivedDomainEvent.builder()
                    .correlationId("RET-" + request.getId())
                    .eventType("RETURN_RECEIVED_DOMAIN")
                    .returnRequest(request)
                    .build();
        }
    }

    @Getter
    @SuperBuilder
    public static class ReturnRequestRefundedDomainEvent extends BaseEvent {
        private final ReturnRequest returnRequest;

        public static ReturnRequestRefundedDomainEvent of(ReturnRequest request) {
            return ReturnRequestRefundedDomainEvent.builder()
                    .correlationId("RET-" + request.getId())
                    .eventType("RETURN_REFUNDED_DOMAIN")
                    .returnRequest(request)
                    .build();
        }
    }

    @Getter
    @SuperBuilder
    public static class ReturnRequestRejectedDomainEvent extends BaseEvent {
        private final ReturnRequest returnRequest;
        private final String reason;

        public static ReturnRequestRejectedDomainEvent of(ReturnRequest request, String reason) {
            return ReturnRequestRejectedDomainEvent.builder()
                    .correlationId("RET-" + request.getId())
                    .eventType("RETURN_REJECTED_DOMAIN")
                    .returnRequest(request)
                    .reason(reason)
                    .build();
        }
    }
}
