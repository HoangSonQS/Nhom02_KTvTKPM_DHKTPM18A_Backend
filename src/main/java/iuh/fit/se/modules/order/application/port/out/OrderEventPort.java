package iuh.fit.se.modules.order.application.port.out;

import iuh.fit.se.shared.event.order.OrderCreatedIntegrationEvent;
import iuh.fit.se.shared.event.order.OrderStatusChangedIntegrationEvent;

public interface OrderEventPort {
    void publishOrderCreated(OrderCreatedIntegrationEvent event);
    void publishOrderStatusChanged(OrderStatusChangedIntegrationEvent event);
}
