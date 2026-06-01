package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.realtime.DataChangedRealtimeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataChangedRealtimeEventListener {

    private static final Set<String> STOREFRONT_ROLES = Set.of("ADMIN", "STAFF_WAREHOUSE", "CUSTOMER", "PUBLIC");
    private static final Set<String> INVENTORY_ROLES = Set.of("ADMIN", "STAFF_WAREHOUSE", "CUSTOMER", "PUBLIC");
    private static final Set<String> LOGISTICS_ROLES = Set.of("ADMIN", "STAFF_WAREHOUSE");
    private static final Set<String> USER_MANAGEMENT_ROLES = Set.of("ADMIN");
    private static final Set<String> ORDER_MANAGEMENT_ROLES = Set.of("ADMIN", "STAFF_SELLER");

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDataChanged(DataChangedRealtimeEvent event) {
        notificationService.publishRealtimeToRoles(rolesFor(event.type()), new RealtimeEventResponse(
                event.type(),
                null,
                event.userId(),
                event.bookId(),
                event.reviewId(),
                event.returnRequestId(),
                event.purchaseOrderId(),
                event.stocktakeId(),
                null,
                null,
                event.status() != null ? event.status() : event.source(),
                event.message(),
                event.occurredAt()
        ));
    }

    private Set<String> rolesFor(String type) {
        return switch (type) {
            case "BOOK_CHANGED", "CATEGORY_CHANGED", "COUPON_CHANGED", "FLASH_SALE_CHANGED" -> STOREFRONT_ROLES;
            case "INVENTORY_INITIALIZED" -> INVENTORY_ROLES;
            case "SUPPLIER_CHANGED", "STOCKTAKE_UPDATED" -> LOGISTICS_ROLES;
            case "CUSTOMER_CONTACT_CHANGED" -> ORDER_MANAGEMENT_ROLES;
            case "USER_CHANGED", "AUDIT_LOG_CHANGED" -> USER_MANAGEMENT_ROLES;
            default -> Set.of("ADMIN");
        };
    }
}
