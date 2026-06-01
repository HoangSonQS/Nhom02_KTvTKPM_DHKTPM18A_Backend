package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.inventory.InventoryStockChangedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class InventoryRealtimeEventListener {

    private static final Set<String> INVENTORY_AFFECTED_ROLES = Set.of("ADMIN", "STAFF_WAREHOUSE", "CUSTOMER", "PUBLIC");

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockChanged(InventoryStockChangedIntegrationEvent event) {
        String type = switch (event.changeType()) {
            case "INITIALIZE" -> "INVENTORY_INITIALIZED";
            case "INCREASE" -> "INVENTORY_INCREASED";
            default -> "INVENTORY_DECREASED";
        };
        notificationService.publishRealtimeToRoles(INVENTORY_AFFECTED_ROLES, new RealtimeEventResponse(
                type,
                null,
                null,
                event.bookId(),
                event.remainingQuantity(),
                null,
                null,
                "Tồn kho sách #" + event.bookId() + " đã thay đổi " + event.amount() + " cuốn",
                event.occurredAt()
        ));
    }
}
