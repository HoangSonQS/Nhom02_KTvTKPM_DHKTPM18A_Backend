package iuh.fit.se.modules.cart.application.service;

import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.modules.cart.application.port.out.CartPersistencePort;
import iuh.fit.se.modules.cart.domain.Cart;
import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.inventory.application.port.in.StockResult;
import iuh.fit.se.modules.promotion.application.port.in.FlashSaleUseCase;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    @Test
    void givenExistingFlashSaleItem_whenAddAgain_thenValidateAndStoreTotalQuantity() {
        CartPersistencePort cartPersistencePort = mock(CartPersistencePort.class);
        BookUseCase bookUseCase = mock(BookUseCase.class);
        InventoryInternalUseCase inventoryInternalUseCase = mock(InventoryInternalUseCase.class);
        FlashSaleUseCase flashSaleUseCase = mock(FlashSaleUseCase.class);
        CartService service = new CartService(cartPersistencePort, bookUseCase, inventoryInternalUseCase, flashSaleUseCase);

        Cart cart = Cart.create(7L);
        cart.addItem(10L, 1, BigDecimal.valueOf(80_000), "Clean Code", 10);
        BookDTO book = BookDTO.builder()
                .id(10L)
                .title("Clean Code")
                .price(BigDecimal.valueOf(100_000))
                .quantity(10)
                .build();
        when(cartPersistencePort.findByUserId(7L)).thenReturn(Optional.of(cart));
        when(bookUseCase.getBook(10L)).thenReturn(book);
        when(inventoryInternalUseCase.getAvailableStock(10L)).thenReturn(StockResult.builder()
                .status(StockResult.Status.SUCCESS)
                .remainingQuantity(10)
                .build());
        when(flashSaleUseCase.reserveActiveSalePrice(10L, 2, BigDecimal.valueOf(100_000)))
                .thenReturn(BigDecimal.valueOf(80_000));

        service.addFlashSaleItem(7L, CartInternalUseCase.AddItemCommand.builder()
                .bookId(10L)
                .quantity(1)
                .build());

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getItems().get(0).getPriceAtAddTime()).isEqualByComparingTo("80000");
        verify(flashSaleUseCase).reserveActiveSalePrice(10L, 2, BigDecimal.valueOf(100_000));
        verify(cartPersistencePort).save(cart);
    }

    @Test
    void givenExistingFlashSaleQuantityAtLimit_whenAddAgain_thenReturnBusinessErrorAndDoNotSave() {
        CartPersistencePort cartPersistencePort = mock(CartPersistencePort.class);
        BookUseCase bookUseCase = mock(BookUseCase.class);
        InventoryInternalUseCase inventoryInternalUseCase = mock(InventoryInternalUseCase.class);
        FlashSaleUseCase flashSaleUseCase = mock(FlashSaleUseCase.class);
        CartService service = new CartService(cartPersistencePort, bookUseCase, inventoryInternalUseCase, flashSaleUseCase);

        Cart cart = Cart.create(7L);
        cart.addItem(10L, 2, BigDecimal.valueOf(80_000), "Clean Code", 10);
        BookDTO book = BookDTO.builder()
                .id(10L)
                .title("Clean Code")
                .price(BigDecimal.valueOf(100_000))
                .quantity(10)
                .build();
        when(cartPersistencePort.findByUserId(7L)).thenReturn(Optional.of(cart));
        when(bookUseCase.getBook(10L)).thenReturn(book);
        when(inventoryInternalUseCase.getAvailableStock(10L)).thenReturn(StockResult.builder()
                .status(StockResult.Status.SUCCESS)
                .remainingQuantity(10)
                .build());
        when(flashSaleUseCase.reserveActiveSalePrice(10L, 3, BigDecimal.valueOf(100_000)))
                .thenThrow(new AppException(ErrorCode.PRM_COUPON_EXPIRED, "Da qua so luong sach dang sale"));

        assertThatThrownBy(() -> service.addFlashSaleItem(7L, CartInternalUseCase.AddItemCommand.builder()
                .bookId(10L)
                .quantity(1)
                .build()))
                .isInstanceOf(AppException.class)
                .hasMessage("Da qua so luong sach dang sale");

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        verify(cartPersistencePort, never()).save(any(Cart.class));
    }
}
