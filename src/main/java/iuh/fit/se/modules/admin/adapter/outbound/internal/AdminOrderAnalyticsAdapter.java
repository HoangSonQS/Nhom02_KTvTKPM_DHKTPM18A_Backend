package iuh.fit.se.modules.admin.adapter.outbound.internal;

import iuh.fit.se.modules.admin.application.port.out.AdminOrderAnalyticsPort;
import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminOrderAnalyticsAdapter implements AdminOrderAnalyticsPort {

    private final OrderInternalUseCase orderInternalUseCase;

    @Override
    public List<TopBookProjection> getTopSellingBooks(int limit) {
        return orderInternalUseCase.getTopSellingBooks(limit).stream()
                .map(item -> new TopBookProjection(
                        item.bookId(),
                        item.title(),
                        item.quantitySold(),
                        item.revenue()
                ))
                .toList();
    }
}
