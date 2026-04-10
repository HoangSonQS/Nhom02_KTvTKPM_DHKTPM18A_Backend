package iuh.fit.se.modules.returns.application.port.out;

import iuh.fit.se.modules.returns.domain.event.ReturnIntegrationEvents.*;

public interface ReturnEventPort {
    void publishReturnCreated(ReturnRequestCreatedIntegrationEvent event);
    void publishReturnApproved(ReturnRequestApprovedIntegrationEvent event);
    void publishReturnReceived(ReturnRequestReceivedIntegrationEvent event);
    void publishReturnRefunded(ReturnRequestRefundedIntegrationEvent event);
    void publishReturnRejected(ReturnRequestRejectedIntegrationEvent event);
}
