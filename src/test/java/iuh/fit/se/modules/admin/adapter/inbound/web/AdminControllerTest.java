package iuh.fit.se.modules.admin.adapter.inbound.web;

import iuh.fit.se.modules.admin.application.port.in.AdminReportUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminControllerTest {

    @Test
    void givenV2DashboardMetrics_whenGetMetrics_thenReturnRevenueAndTopBooksContract() {
        AdminReportUseCase useCase = mock(AdminReportUseCase.class);
        AdminReportUseCase.DashboardV2Dto metrics = AdminReportUseCase.DashboardV2Dto.builder()
                .totalOrders(10)
                .paidOrders(8)
                .totalRevenue(new BigDecimal("1000000"))
                .refundAmount(new BigDecimal("100000"))
                .netRevenue(new BigDecimal("900000"))
                .topBooks(List.of(new AdminReportUseCase.TopBookDto(1L, "Clean Code", 5, new BigDecimal("500000"))))
                .build();
        when(useCase.getDashboardMetricsV2()).thenReturn(metrics);

        AdminController controller = new AdminController(useCase);

        ResponseEntity<ApiResponse<AdminReportUseCase.DashboardV2Dto>> response = controller.getMetrics();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isSameAs(metrics);
        assertThat(response.getBody().getData().getTotalRevenue()).isEqualByComparingTo("1000000");
        assertThat(response.getBody().getData().getNetRevenue()).isEqualByComparingTo("900000");
        assertThat(response.getBody().getData().getTopBooks()).hasSize(1);
        verify(useCase).getDashboardMetricsV2();
    }

    @Test
    void adminDashboardControllerUsesV2Mapping() {
        RequestMapping mapping = AdminController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/v2/admin/dashboard");
    }
}
