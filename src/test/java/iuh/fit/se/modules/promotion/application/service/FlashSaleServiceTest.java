package iuh.fit.se.modules.promotion.application.service;

import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.promotion.application.port.in.FlashSaleUseCase;
import iuh.fit.se.modules.promotion.application.port.out.FlashSalePersistencePort;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlashSaleServiceTest {

    @Test
    void givenSameBookAlreadyInOverlappingSale_whenCreate_thenRejectDuplicateFlashSale() {
        FlashSalePersistencePort persistencePort = mock(FlashSalePersistencePort.class);
        BookUseCase bookUseCase = mock(BookUseCase.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        FlashSaleService service = new FlashSaleService(persistencePort, bookUseCase, eventPublisher);
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 2, 8, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 6, 2, 12, 0);
        FlashSaleUseCase.FlashSaleCommand command = new FlashSaleUseCase.FlashSaleCommand(
                10L,
                5,
                20,
                startAt,
                endAt,
                true
        );
        when(bookUseCase.getBook(10L)).thenReturn(BookDTO.builder()
                .id(10L)
                .title("Clean Code")
                .price(BigDecimal.valueOf(100_000))
                .quantity(10)
                .build());
        when(persistencePort.existsOverlappingActiveSale(10L, startAt, endAt)).thenReturn(true);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT))
                .hasMessage("Sach nay da co trong su kien Flash Sale cung khung gio");

        verify(persistencePort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
