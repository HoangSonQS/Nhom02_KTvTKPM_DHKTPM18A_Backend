package iuh.fit.se.modules.admin.application.port.in;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Port/In cho các báo cáo quản trị.
 */
public interface AdminReportUseCase {
    DashboardDto getDashboardMetrics();

    @Getter
    @Builder
    class DashboardDto {
        private long totalOrders;
        private long paidOrders;
        private double conversionRate;
        private double averageTimeToPaymentSeconds;
        private BigDecimal averageOrderValue;
        private long uniqueBuyers;
    }
}
