package iuh.fit.se.modules.order.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderAdminTransitionTest {

    @Test
    void givenPendingOrder_whenAdminConfirms_thenTransitionIsAllowed() {
        assertThat(Order.isValidAdminTransition(FulfillmentStatus.PENDING, FulfillmentStatus.CONFIRMED))
                .isTrue();
    }
}
