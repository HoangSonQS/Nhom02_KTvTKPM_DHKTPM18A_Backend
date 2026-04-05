package iuh.fit.se.modules.admin.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.modules.admin.adapter.outbound.persistence.OrderReportRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "APIs cho báo cáo và giám sát hệ thống (CQRS)")
@PreAuthorize("hasAuthority('DASHBOARD_REVENUE') or hasAuthority('DASHBOARD_INVENTORY') or hasAuthority('DASHBOARD_FULL')")
public class AdminController {

    private final OrderReportRepository repository;

    @GetMapping("/metrics")
    @Operation(summary = "Lấy các chỉ số kinh doanh chính (AOV, ATTP, Conversion)")
    public ResponseEntity<DashboardResponse> getMetrics() {
        long totalOrders = repository.count();
        long paidOrders = repository.countPaidOrders();
        
        Double attp = repository.calculateAverageTimeToPaymentSeconds();
        BigDecimal aov = repository.calculateAverageOrderValue();
        long uniqueBuyers = repository.countUniqueBuyers();

        double conversionRate = totalOrders > 0 ? (double) paidOrders / totalOrders * 100 : 0;

        return ResponseEntity.ok(DashboardResponse.builder()
                .totalOrders(totalOrders)
                .paidOrders(paidOrders)
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .averageTimeToPaymentSeconds(attp != null ? Math.round(attp * 100.0) / 100.0 : 0.0)
                .averageOrderValue(aov != null ? aov : BigDecimal.ZERO)
                .uniqueBuyers(uniqueBuyers)
                .build());
    }

    @Data
    @Builder
    public static class DashboardResponse {
        private long totalOrders;
        private long paidOrders;
        private double conversionRate;
        private double averageTimeToPaymentSeconds;
        private BigDecimal averageOrderValue;
        private long uniqueBuyers;
    }
}
