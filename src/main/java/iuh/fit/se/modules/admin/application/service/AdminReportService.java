package iuh.fit.se.modules.admin.application.service;

import iuh.fit.se.modules.admin.application.port.in.AdminReportUseCase;
import iuh.fit.se.modules.admin.application.port.out.AdminOrderAnalyticsPort;
import iuh.fit.se.modules.admin.application.port.out.OrderReportPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportService implements AdminReportUseCase {

    private final OrderReportPersistencePort reportPersistencePort;
    private final AdminOrderAnalyticsPort orderAnalyticsPort;

    @Override
    @Cacheable(value = "dashboardStats", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('dashboardStats', 'metrics')")
    public DashboardDto getDashboardMetrics() {
        DashboardMetrics metrics = loadBaseMetrics();

        return DashboardDto.builder()
                .totalOrders(metrics.totalOrders())
                .paidOrders(metrics.paidOrders())
                .conversionRate(metrics.conversionRate())
                .averageTimeToPaymentSeconds(metrics.averageTimeToPaymentSeconds())
                .averageOrderValue(metrics.averageOrderValue())
                .uniqueBuyers(metrics.uniqueBuyers())
                .build();
    }

    @Override
    @Cacheable(value = "dashboardStats", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('dashboardStats', 'metrics-v2')")
    public DashboardV2Dto getDashboardMetricsV2() {
        DashboardMetrics metrics = loadBaseMetrics();
        BigDecimal totalRevenue = zeroIfNull(reportPersistencePort.calculateTotalRevenue());
        BigDecimal refundAmount = zeroIfNull(reportPersistencePort.calculateRefundAmount());
        List<TopBookDto> topBooks = orderAnalyticsPort.getTopSellingBooks(5).stream()
                .map(book -> new TopBookDto(
                        book.bookId(),
                        book.title(),
                        book.quantitySold(),
                        book.revenue()
                ))
                .toList();

        return DashboardV2Dto.builder()
                .totalOrders(metrics.totalOrders())
                .paidOrders(metrics.paidOrders())
                .conversionRate(metrics.conversionRate())
                .averageTimeToPaymentSeconds(metrics.averageTimeToPaymentSeconds())
                .averageOrderValue(metrics.averageOrderValue())
                .uniqueBuyers(metrics.uniqueBuyers())
                .totalRevenue(totalRevenue)
                .refundAmount(refundAmount)
                .netRevenue(totalRevenue.subtract(refundAmount))
                .topBooks(topBooks)
                .build();
    }

    private DashboardMetrics loadBaseMetrics() {
        long totalOrders = reportPersistencePort.count();
        long paidOrders = reportPersistencePort.countPaidOrders();
        double attp = reportPersistencePort.calculateAverageTimeToPaymentSeconds();
        BigDecimal aov = zeroIfNull(reportPersistencePort.calculateAverageOrderValue());
        long uniqueBuyers = reportPersistencePort.countUniqueBuyers();
        double conversionRate = totalOrders > 0 ? (double) paidOrders / totalOrders * 100 : 0;

        return new DashboardMetrics(
                totalOrders,
                paidOrders,
                Math.round(conversionRate * 100.0) / 100.0,
                Math.round(attp * 100.0) / 100.0,
                aov,
                uniqueBuyers
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record DashboardMetrics(
            long totalOrders,
            long paidOrders,
            double conversionRate,
            double averageTimeToPaymentSeconds,
            BigDecimal averageOrderValue,
            long uniqueBuyers
    ) {}
}
