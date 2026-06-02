package iuh.fit.se.modules.home.application.service;

import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import iuh.fit.se.modules.home.application.port.in.HomeDiscoveryUseCase;
import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeDiscoveryServiceTest {

    @Mock
    private BookUseCase bookUseCase;

    @Mock
    private OrderInternalUseCase orderUseCase;

    @InjectMocks
    private HomeDiscoveryService service;

    @Test
    void givenDiscountedBooks_whenGetShockSaleBooks_thenOnlyReturnDiscountAtLeastThirtyPercent() {
        when(bookUseCase.searchBooks(null, null)).thenReturn(List.of(
                book(1L, "Small Sale", "100000", "120000", 5, 4),
                book(2L, "Big Sale", "60000", "120000", 3, 2),
                book(3L, "No Sale", "90000", null, 7, 5)
        ));
        when(orderUseCase.getTopSellingBooks(anyInt())).thenReturn(List.of());

        List<HomeDiscoveryUseCase.HomeBookResponse> result = service.getShockSaleBooks(5);

        assertThat(result).extracting(HomeDiscoveryUseCase.HomeBookResponse::id).containsExactly(2L);
        assertThat(result.get(0).discountPercent()).isEqualTo(50);
        assertThat(result.get(0).badge()).isEqualTo("SALE");
    }

    @Test
    void givenMonthlySalesRows_whenGetHotBooks_thenReturnCatalogDetailsInSalesOrder() {
        LocalDate today = LocalDate.now();
        when(orderUseCase.getBookSales(eq(today.withDayOfMonth(1)), eq(today))).thenReturn(List.of(
                new OrderInternalUseCase.BookSalesResponse(2L, "Book 2", 12L, new BigDecimal("720000")),
                new OrderInternalUseCase.BookSalesResponse(1L, "Book 1", 5L, new BigDecimal("500000"))
        ));
        when(bookUseCase.searchBooks(null, null)).thenReturn(List.of(
                book(1L, "Book 1", "100000", "120000", 5, 4),
                book(2L, "Book 2", "60000", "90000", 3, 2)
        ));

        List<HomeDiscoveryUseCase.HomeBookResponse> result = service.getHotBooks(2);

        assertThat(result).extracting(HomeDiscoveryUseCase.HomeBookResponse::id).containsExactly(2L, 1L);
        assertThat(result.get(0).quantitySold()).isEqualTo(12L);
        assertThat(result.get(0).revenue()).isEqualByComparingTo("720000");
    }

    @Test
    void givenYesterdaySalesRows_whenGetTrendingBooks_thenReturnYesterdaySoldBooksOnly() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(orderUseCase.getBookSales(eq(yesterday), eq(yesterday))).thenReturn(List.of(
                new OrderInternalUseCase.BookSalesResponse(1L, "Book 1", 3L, new BigDecimal("300000"))
        ));
        when(bookUseCase.searchBooks(null, null)).thenReturn(List.of(
                book(1L, "Book 1", "100000", "120000", 5, 4),
                book(2L, "Book 2", "60000", "90000", 3, 2)
        ));

        List<HomeDiscoveryUseCase.HomeBookResponse> result = service.getTrendingBooks(5);

        assertThat(result).extracting(HomeDiscoveryUseCase.HomeBookResponse::id).containsExactly(1L);
        assertThat(result.get(0).quantitySold()).isEqualTo(3L);
    }

    @Test
    void givenBestSellerRows_whenGetRankingBooks_thenOnlyReturnBooksSoldAtLeastTwentyCopies() {
        when(orderUseCase.getBookSales(null, null)).thenReturn(List.of(
                new OrderInternalUseCase.BookSalesResponse(1L, "Book 1", 25L, new BigDecimal("2500000")),
                new OrderInternalUseCase.BookSalesResponse(2L, "Book 2", 19L, new BigDecimal("1900000")),
                new OrderInternalUseCase.BookSalesResponse(3L, "Book 3", 20L, new BigDecimal("2000000"))
        ));
        when(bookUseCase.searchBooks(null, null)).thenReturn(List.of(
                book(1L, "Book 1", "100000", "120000", 5, 4),
                book(2L, "Book 2", "100000", "120000", 5, 4),
                book(3L, "Book 3", "100000", "120000", 5, 4),
                book(4L, "Fallback Book", "100000", "120000", 5, 4)
        ));

        List<HomeDiscoveryUseCase.HomeBookResponse> result = service.getRankingBooks("BEST_SELLER", 20);

        assertThat(result).extracting(HomeDiscoveryUseCase.HomeBookResponse::id).containsExactly(1L, 3L);
        assertThat(result).extracting(HomeDiscoveryUseCase.HomeBookResponse::quantitySold).containsExactly(25L, 20L);
    }

    private BookDTO book(Long id, String title, String price, String originalPrice, int quantity, int ratingCount) {
        return BookDTO.builder()
                .id(id)
                .title(title)
                .author("Author")
                .description("Description")
                .price(new BigDecimal(price))
                .originalPrice(originalPrice != null ? new BigDecimal(originalPrice) : null)
                .quantity(quantity)
                .imageUrl("/book.png")
                .isActive(true)
                .averageRating(new BigDecimal("4.5"))
                .ratingCount(ratingCount)
                .categoryIds(Set.of(1L))
                .build();
    }
}
