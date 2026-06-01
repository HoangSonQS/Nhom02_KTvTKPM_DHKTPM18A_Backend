package iuh.fit.se.modules.returns.adapter.inbound.web;

import iuh.fit.se.modules.returns.application.port.in.ReturnRequestUseCase;
import iuh.fit.se.modules.returns.domain.ReturnReason;
import iuh.fit.se.modules.returns.domain.ReturnRequest;
import iuh.fit.se.modules.returns.domain.ReturnStatus;
import iuh.fit.se.shared.config.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReturnRequestControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenGetMyReturns_thenUseAuthenticatedCustomerId() {
        ReturnRequestUseCase useCase = mock(ReturnRequestUseCase.class);
        when(useCase.getByCustomer(42L)).thenReturn(List.of());
        authenticateAs(42L);

        new ReturnRequestController(useCase).getMyReturns();

        verify(useCase).getByCustomer(42L);
    }

    @Test
    void whenCreateReturn_thenUseAuthenticatedCustomerId() {
        ReturnRequestUseCase useCase = mock(ReturnRequestUseCase.class);
        when(useCase.createReturn(any())).thenReturn(ReturnRequest.builder()
                .id("RET-42")
                .orderId(12L)
                .customerId(42L)
                .status(ReturnStatus.PENDING)
                .reason(ReturnReason.DEFECTIVE)
                .items(new ArrayList<>())
                .histories(new ArrayList<>())
                .build());
        authenticateAs(42L);
        CreateReturnRequestDTO dto = new CreateReturnRequestDTO();
        dto.setOrderId(12L);
        dto.setReason(ReturnReason.DEFECTIVE);
        dto.setItems(List.of());

        new ReturnRequestController(useCase).createReturn(dto);

        ArgumentCaptor<ReturnRequestUseCase.CreateReturnCommand> command =
                ArgumentCaptor.forClass(ReturnRequestUseCase.CreateReturnCommand.class);
        verify(useCase).createReturn(command.capture());
        assertThat(command.getValue().getCustomerId()).isEqualTo(42L);
    }

    private void authenticateAs(Long userId) {
        UserPrincipal principal = new UserPrincipal(
                "customer@sebook.local",
                Map.of("userId", userId, "role", "CUSTOMER")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}
