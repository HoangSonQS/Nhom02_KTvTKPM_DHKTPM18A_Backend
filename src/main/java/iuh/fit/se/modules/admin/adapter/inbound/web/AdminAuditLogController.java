package iuh.fit.se.modules.admin.adapter.inbound.web;

import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.audit.application.port.in.AuditLogQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DEAD_LETTER_READ') or hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AuditLogQueryUseCase auditLogQueryUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogQueryUseCase.AuditLogResponse>>> listRecentLogs() {
        return ResponseEntity.ok(ApiResponse.success(auditLogQueryUseCase.listRecentLogs()));
    }
}
