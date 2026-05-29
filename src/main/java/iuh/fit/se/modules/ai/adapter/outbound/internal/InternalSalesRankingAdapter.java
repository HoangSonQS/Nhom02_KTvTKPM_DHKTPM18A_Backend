package iuh.fit.se.modules.ai.adapter.outbound.internal;

import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookContext;
import iuh.fit.se.modules.ai.application.port.out.SalesRankingPort;
import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class InternalSalesRankingAdapter implements SalesRankingPort {

    private final OrderInternalUseCase orderUseCase;
    private final CatalogBookPort catalogBookPort;

    @Override
    public List<BookContext> getTopSellingBooks(int limit) {
        int effectiveLimit = limit > 0 ? limit : 5;
        return orderUseCase.getTopSellingBooks(effectiveLimit).stream()
                .map(OrderInternalUseCase.TopSellingBookResponse::bookId)
                .map(this::getBookSafely)
                .filter(Objects::nonNull)
                .filter(BookContext::isActive)
                .toList();
    }

    private BookContext getBookSafely(Long bookId) {
        try {
            return catalogBookPort.getBook(bookId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
