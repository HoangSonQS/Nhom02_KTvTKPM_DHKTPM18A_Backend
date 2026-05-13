package iuh.fit.se.modules.admin.adapter.inbound.web;

import iuh.fit.se.modules.admin.application.port.in.AdminReportUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DASHBOARD_REVENUE') or hasAuthority('DASHBOARD_INVENTORY') or hasAuthority('DASHBOARD_FULL')")
public class AdminV2Controller {

    private final AdminReportUseCase adminReportUseCase;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<AdminReportUseCase.DashboardV2Dto>> getMetricsV2() {
        return ResponseEntity.ok(ApiResponse.success(adminReportUseCase.getDashboardMetricsV2()));
    }
}
