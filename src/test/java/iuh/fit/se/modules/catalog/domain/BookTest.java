package iuh.fit.se.modules.catalog.domain;

import iuh.fit.se.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Domain Test cho Book.
 * Kiểm tra logic nghiệp vụ thuần túy của thực thể.
 */
class BookTest {

    @Test
    void whenDecreaseStockValidAmount_thenQuantityUpdated() {
        Book book = Book.builder().quantity(10).build();
        book.decreaseStock(3);
        assertThat(book.getQuantity()).isEqualTo(7);
    }

    @Test
    void whenDecreaseStockOverAmount_thenThrowsException() {
        Book book = Book.builder().quantity(10).build();
        assertThatThrownBy(() -> book.decreaseStock(11))
                .isInstanceOf(AppException.class);
    }

    @Test
    void whenIncreaseStock_thenQuantityUpdated() {
        Book book = Book.builder().quantity(10).build();
        book.increaseStock(5);
        assertThat(book.getQuantity()).isEqualTo(15);
    }
}
