package iuh.fit.se.modules.admin.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Dashboard", description = "APIs cho báo cáo và giám sát hệ thống (CQRS)")
@PreAuthorize("hasAuthority('DASHBOARD_REVENUE') or hasAuthority('DASHBOARD_INVENTORY') or hasAuthority('DASHBOARD_FULL')")
public class AdminController {

    private final AdminReportUseCase adminReportUseCase;

    @GetMapping("/metrics")
    @Operation(summary = "Lấy các chỉ số kinh doanh chính (AOV, ATTP, Conversion)")
    public ResponseEntity<ApiResponse<AdminReportUseCase.DashboardV2Dto>> getMetrics() {
        return ResponseEntity.ok(ApiResponse.success(adminReportUseCase.getDashboardMetricsV2()));
    }
}
