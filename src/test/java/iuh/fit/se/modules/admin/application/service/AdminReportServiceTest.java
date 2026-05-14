package iuh.fit.se.modules.admin.application.service;

import iuh.fit.se.modules.admin.application.port.in.AdminReportUseCase;
import iuh.fit.se.modules.admin.application.port.out.AdminOrderAnalyticsPort;
import iuh.fit.se.modules.admin.application.port.out.OrderReportPersistencePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminReportServiceTest {

    @Test
    void givenEmptyReports_whenGetDashboardMetricsV2_thenReturnZeroMetricsWithoutError() {
        OrderReportPersistencePort reportPersistencePort = mock(OrderReportPersistencePort.class);
        AdminOrderAnalyticsPort orderAnalyticsPort = mock(AdminOrderAnalyticsPort.class);
        when(reportPersistencePort.count()).thenReturn(0L);
        when(reportPersistencePort.countPaidOrders()).thenReturn(0L);
        when(reportPersistencePort.calculateAverageTimeToPaymentSeconds()).thenReturn(0.0);
        when(reportPersistencePort.calculateAverageOrderValue()).thenReturn(null);
        when(reportPersistencePort.countUniqueBuyers()).thenReturn(0L);
        when(reportPersistencePort.calculateTotalRevenue()).thenReturn(null);
        when(reportPersistencePort.calculateRefundAmount()).thenReturn(null);
        when(orderAnalyticsPort.getTopSellingBooks(5)).thenReturn(List.of());

        AdminReportService service = new AdminReportService(reportPersistencePort, orderAnalyticsPort);

        AdminReportUseCase.DashboardV2Dto metrics = service.getDashboardMetricsV2();

        assertThat(metrics.getTotalOrders()).isZero();
        assertThat(metrics.getPaidOrders()).isZero();
        assertThat(metrics.getConversionRate()).isZero();
        assertThat(metrics.getAverageTimeToPaymentSeconds()).isZero();
        assertThat(metrics.getAverageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.getRefundAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.getNetRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.getTopBooks()).isEmpty();
    }
}
