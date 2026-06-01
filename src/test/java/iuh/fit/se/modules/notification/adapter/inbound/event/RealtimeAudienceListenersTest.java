package iuh.fit.se.modules.notification.adapter.inbound.event;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.service.NotificationService;
import iuh.fit.se.shared.event.inventory.InventoryStockChangedIntegrationEvent;
import iuh.fit.se.shared.event.realtime.DataChangedRealtimeEvent;
import iuh.fit.se.shared.event.realtime.PurchaseOrderRealtimeEvent;
import iuh.fit.se.shared.event.realtime.ReturnRealtimeEvent;
import iuh.fit.se.shared.event.realtime.ReviewRealtimeEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeAudienceListenersTest {

    @Test
    void inventoryChanged_shouldReachStorefrontRolesWithBookAndQuantity() {
        NotificationService notificationService = mock(NotificationService.class);
        InventoryRealtimeEventListener listener = new InventoryRealtimeEventListener(notificationService);
        ArgumentCaptor<RealtimeEventResponse> payload = ArgumentCaptor.forClass(RealtimeEventResponse.class);

        listener.handleStockChanged(InventoryStockChangedIntegrationEvent.of(7L, 3, 12, "INITIALIZE"));

        verify(notificationService).publishRealtimeToRoles(
                eq(Set.of("ADMIN", "STAFF_WAREHOUSE", "CUSTOMER", "PUBLIC")),
                payload.capture()
        );
        assertThat(payload.getValue().type()).isEqualTo("INVENTORY_INITIALIZED");
        assertThat(payload.getValue().bookId()).isEqualTo(7L);
        assertThat(payload.getValue().quantity()).isEqualTo(12);
    }

    @Test
    void returnCreated_shouldReachCustomerAndManagementRolesWithReturnRequestId() {
        NotificationService notificationService = mock(NotificationService.class);
        ReturnRealtimeEventListener listener = new ReturnRealtimeEventListener(notificationService);
        ArgumentCaptor<RealtimeEventResponse> payload = ArgumentCaptor.forClass(RealtimeEventResponse.class);

        listener.handleReturnRealtime(ReturnRealtimeEvent.created("RET-12", 12L, 5L));

        verify(notificationService).publishRealtimeToUser(eq(5L), payload.capture());
        verify(notificationService).publishRealtimeToRoles(
                eq(Set.of("ADMIN", "STAFF_SELLER")),
                eq(payload.getValue())
        );
        assertThat(payload.getValue().type()).isEqualTo("RETURN_CREATED");
        assertThat(payload.getValue().returnRequestId()).isEqualTo("RET-12");
        assertThat(payload.getValue().orderId()).isEqualTo(12L);
    }

    @Test
    void reviewDeleted_shouldReachPublicWithReviewAndBookIds() {
        NotificationService notificationService = mock(NotificationService.class);
        ReviewRealtimeEventListener listener = new ReviewRealtimeEventListener(notificationService);
        ArgumentCaptor<RealtimeEventResponse> payload = ArgumentCaptor.forClass(RealtimeEventResponse.class);

        listener.handleReviewRealtime(ReviewRealtimeEvent.deleted(9L, 7L, 5L));

        verify(notificationService).publishRealtimeToRoles(
                eq(Set.of("ADMIN", "CUSTOMER", "PUBLIC")),
                payload.capture()
        );
        assertThat(payload.getValue().type()).isEqualTo("REVIEW_DELETED");
        assertThat(payload.getValue().reviewId()).isEqualTo(9L);
        assertThat(payload.getValue().bookId()).isEqualTo(7L);
    }

    @Test
    void purchaseOrderUpdated_shouldUsePurchaseOrderIdInsteadOfOrderId() {
        NotificationService notificationService = mock(NotificationService.class);
        PurchaseOrderRealtimeEventListener listener = new PurchaseOrderRealtimeEventListener(notificationService);
        ArgumentCaptor<RealtimeEventResponse> payload = ArgumentCaptor.forClass(RealtimeEventResponse.class);

        listener.handlePurchaseOrderRealtime(PurchaseOrderRealtimeEvent.updated(21L, "RECEIVED", "PO received"));

        verify(notificationService).publishRealtimeToRoles(
                eq(Set.of("ADMIN", "STAFF_WAREHOUSE")),
                payload.capture()
        );
        assertThat(payload.getValue().purchaseOrderId()).isEqualTo(21L);
        assertThat(payload.getValue().orderId()).isNull();
    }

    @Test
    void stocktakeUpdated_shouldReachWarehouseRolesWithStocktakeId() {
        NotificationService notificationService = mock(NotificationService.class);
        DataChangedRealtimeEventListener listener = new DataChangedRealtimeEventListener(notificationService);
        ArgumentCaptor<RealtimeEventResponse> payload = ArgumentCaptor.forClass(RealtimeEventResponse.class);

        listener.handleDataChanged(DataChangedRealtimeEvent.stocktake(42L, "APPROVED", "Stocktake approved"));

        verify(notificationService).publishRealtimeToRoles(
                eq(Set.of("ADMIN", "STAFF_WAREHOUSE")),
                payload.capture()
        );
        assertThat(payload.getValue().type()).isEqualTo("STOCKTAKE_UPDATED");
        assertThat(payload.getValue().stocktakeId()).isEqualTo(42L);
        assertThat(payload.getValue().status()).isEqualTo("APPROVED");
    }
}
