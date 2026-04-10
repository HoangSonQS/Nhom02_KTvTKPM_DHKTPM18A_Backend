package iuh.fit.se.modules.admin.application.service;

import iuh.fit.se.modules.admin.adapter.outbound.persistence.OrderReportRepository;
import iuh.fit.se.modules.admin.application.port.in.AdminReportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service xử lý báo cáo quản trị với cơ chế Cache hiệu năng cao.
 */
@Service
@RequiredArgsConstructor
public class AdminReportService implements AdminReportUseCase {

    private final OrderReportRepository repository;

    @Override
    @Cacheable(value = "dashboardStats", key = "T(iuh.fit.se.shared.cache.CacheKeyUtility).createSaltedKey('dashboardStats', 'metrics')")
    public DashboardDto getDashboardMetrics() {
        long totalOrders = repository.count();
        long paidOrders = repository.countPaidOrders();
        
        Double attp = repository.calculateAverageTimeToPaymentSeconds();
        BigDecimal aov = repository.calculateAverageOrderValue();
        long uniqueBuyers = repository.countUniqueBuyers();

        double conversionRate = totalOrders > 0 ? (double) paidOrders / totalOrders * 100 : 0;

        return DashboardDto.builder()
                .totalOrders(totalOrders)
                .paidOrders(paidOrders)
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .averageTimeToPaymentSeconds(attp != null ? Math.round(attp * 100.0) / 100.0 : 0.0)
                .averageOrderValue(aov != null ? aov : BigDecimal.ZERO)
                .uniqueBuyers(uniqueBuyers)
                .build();
    }
}
