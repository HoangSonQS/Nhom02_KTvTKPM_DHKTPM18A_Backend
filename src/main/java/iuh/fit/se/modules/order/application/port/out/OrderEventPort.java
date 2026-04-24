package iuh.fit.se.modules.order.application.port.out;

import iuh.fit.se.shared.event.order.OrderCreatedIntegrationEvent;

public interface OrderEventPort {
    void publishOrderCreated(OrderCreatedIntegrationEvent event);
}
