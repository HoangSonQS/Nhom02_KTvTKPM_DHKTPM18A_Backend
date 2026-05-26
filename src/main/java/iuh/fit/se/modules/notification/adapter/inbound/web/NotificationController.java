package iuh.fit.se.modules.notification.adapter.inbound.web;

import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;
import iuh.fit.se.modules.notification.application.port.in.NotificationCustomerUseCase;
import iuh.fit.se.modules.notification.application.service.NotificationRealtimeService;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.config.JwtTokenProvider;
import iuh.fit.se.shared.security.SecurityUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationCustomerUseCase notificationCustomerUseCase;
    private final NotificationRealtimeService notificationRealtimeService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerNotificationResponse>>> getMyNotifications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "Danh sach thong bao",
                notificationCustomerUseCase.getMyNotifications(userId)
        ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countUnread() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                "So thong bao chua doc",
                Map.of("count", notificationCustomerUseCase.countUnread(userId))
        ));
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String token) {
        Claims claims = jwtTokenProvider.parseToken(token);
        Object userId = claims.get("userId");
        if (!(userId instanceof Number number)) {
            throw new IllegalArgumentException("Token khong hop le");
        }
        return notificationRealtimeService.subscribe(number.longValue());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationCustomerUseCase.markAsRead(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Da doc thong bao", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationCustomerUseCase.markAllAsRead(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Da doc tat ca thong bao", null));
    }
}
