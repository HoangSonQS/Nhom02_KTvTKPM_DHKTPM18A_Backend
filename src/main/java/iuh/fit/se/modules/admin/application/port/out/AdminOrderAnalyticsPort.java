package iuh.fit.se.modules.admin.application.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface AdminOrderAnalyticsPort {
    List<TopBookProjection> getTopSellingBooks(int limit);

    record TopBookProjection(
            Long bookId,
            String title,
            long quantitySold,
            BigDecimal revenue
    ) {}
}
