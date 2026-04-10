package iuh.fit.se.modules.order.application.port.out;

import iuh.fit.se.modules.order.application.event.OrderCreatedIntegrationEvent;

public interface OrderEventPort {
    void publishOrderCreated(OrderCreatedIntegrationEvent event);
}
