package iuh.fit.se.modules.order.adapter.inbound.web;

import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.order.domain.FulfillmentStatus;
import iuh.fit.se.shared.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderAdminControllerTest {

    @Test
    void givenV1AdminOrders_whenSearchWithStatusAndCustomerKeyword_thenDelegatesSearchCriteria() {
        OrderInternalUseCase useCase = mock(OrderInternalUseCase.class);
        OrderInternalUseCase.AdminOrderResponse order = OrderInternalUseCase.AdminOrderResponse.builder()
                .orderId(1L)
                .customerName("Nguyen Van A")
                .fulfillmentStatus(FulfillmentStatus.PROCESSING.name())
                .build();
        when(useCase.searchAdminOrders(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(order));

        OrderAdminController controller = new OrderAdminController(useCase);

        ResponseEntity<ApiResponse<List<OrderInternalUseCase.AdminOrderResponse>>> response =
                controller.searchOrders(FulfillmentStatus.PROCESSING, "nguyen");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsExactly(order);
        verify(useCase).searchAdminOrders(argThat(criteria ->
                criteria.getStatus() == FulfillmentStatus.PROCESSING
                        && "nguyen".equals(criteria.getCustomerKeyword())));
    }
}
