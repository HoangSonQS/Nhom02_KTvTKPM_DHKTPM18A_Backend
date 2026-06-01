package iuh.fit.se.modules.returns.application.service;

import iuh.fit.se.modules.returns.application.port.in.ReturnRequestUseCase.CreateReturnCommand;
import iuh.fit.se.modules.returns.application.port.out.OrderQueryPort;
import iuh.fit.se.modules.returns.application.port.out.ReturnEvidenceImagePort;
import iuh.fit.se.modules.returns.application.port.out.ReturnRequestRepository;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private OrderQueryPort orderQueryPort;

    @Mock
    private ReturnEvidenceImagePort returnEvidenceImagePort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReturnRequestService service;

    @Test
    void whenOrderBelongsToAnotherCustomer_thenRejectReturnCreation() {
        when(orderQueryPort.findOrderById(16L)).thenReturn(Optional.of(
                OrderQueryPort.OrderDto.builder()
                        .orderId(16L)
                        .customerId(9L)
                        .build()
        ));

        AppException exception = assertThrows(AppException.class, () -> service.createReturn(
                CreateReturnCommand.builder()
                        .orderId(16L)
                        .customerId(42L)
                        .build()
        ));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verifyNoInteractions(returnRequestRepository);
    }
}
