package iuh.fit.se.modules.admin.adapter.inbound.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.modules.notification.application.port.in.NotificationAdminPort;
import iuh.fit.se.modules.notification.application.port.in.NotificationLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dead-letter")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Dead Letter", description = "Quản lý các sự kiện thông báo bị lỗi vĩnh viễn (Staff+ Operational Support)")
public class AdminDeadLetterController {

    private final NotificationAdminPort notificationAdminPort;

    @PreAuthorize("hasAuthority('DEAD_LETTER_READ')")
    @GetMapping("/failed-notifications")
    @Operation(summary = "Lấy danh sách các thông báo lỗi vĩnh viễn")
    public ResponseEntity<List<NotificationLogResponse>> getFailedNotifications() {
        return ResponseEntity.ok(notificationAdminPort.getFailedNotifications());
    }

    @PreAuthorize("hasAuthority('DEAD_LETTER_RETRY')")
    @PostMapping("/retry/{logId}")
    @Operation(summary = "Yêu cầu phát lại sự kiện (Manual Retry)")
    public ResponseEntity<String> retryNotification(@PathVariable Long logId) {
        notificationAdminPort.retryNotification(logId);
        return ResponseEntity.ok("Retry request submitted successfully. The log has been cleared for reprocessing.");
    }
}
