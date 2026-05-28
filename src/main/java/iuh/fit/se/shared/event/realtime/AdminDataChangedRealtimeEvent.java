package iuh.fit.se.shared.event.realtime;

import java.time.LocalDateTime;

public record AdminDataChangedRealtimeEvent(
        String source,
        String message,
        LocalDateTime occurredAt
) {
    public static AdminDataChangedRealtimeEvent of(String source, String message) {
        return new AdminDataChangedRealtimeEvent(source, message, LocalDateTime.now());
    }
}
