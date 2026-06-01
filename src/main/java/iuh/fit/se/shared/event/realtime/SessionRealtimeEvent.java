package iuh.fit.se.shared.event.realtime;

import java.time.LocalDateTime;

public record SessionRealtimeEvent(
        String type,
        Long userId,
        String activeDeviceId,
        String message,
        LocalDateTime occurredAt
) {
    public static SessionRealtimeEvent expiredByNewLogin(Long userId, String activeDeviceId) {
        return new SessionRealtimeEvent(
                "SESSION_EXPIRED",
                userId,
                activeDeviceId,
                "Phiên đăng nhập đã hết hạn vui lòng đăng nhập lại.",
                LocalDateTime.now()
        );
    }

    public static SessionRealtimeEvent expiredByAdminLock(Long userId) {
        return new SessionRealtimeEvent(
                "SESSION_EXPIRED",
                userId,
                null,
                "Tai khoan da bi khoa. Vui long lien he quan tri vien.",
                LocalDateTime.now()
        );
    }
}
