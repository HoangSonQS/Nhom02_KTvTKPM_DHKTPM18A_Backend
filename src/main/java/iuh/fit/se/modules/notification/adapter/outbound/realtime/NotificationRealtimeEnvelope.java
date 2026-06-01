package iuh.fit.se.modules.notification.adapter.outbound.realtime;

import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;

import java.util.Set;

public record NotificationRealtimeEnvelope(
        String payloadType,
        String target,
        Long userId,
        Set<String> roles,
        String excludedDeviceId,
        CustomerNotificationResponse notification,
        RealtimeEventResponse event
) {
    public static final String PAYLOAD_NOTIFICATION = "notification";
    public static final String PAYLOAD_REALTIME = "realtime";
    public static final String TARGET_USER = "USER";
    public static final String TARGET_USER_EXCEPT_DEVICE = "USER_EXCEPT_DEVICE";
    public static final String TARGET_ROLES = "ROLES";

    public static NotificationRealtimeEnvelope notification(Long userId, CustomerNotificationResponse notification) {
        return new NotificationRealtimeEnvelope(PAYLOAD_NOTIFICATION, TARGET_USER, userId, Set.of(), null, notification, null);
    }

    public static NotificationRealtimeEnvelope eventToUser(Long userId, RealtimeEventResponse event) {
        return new NotificationRealtimeEnvelope(PAYLOAD_REALTIME, TARGET_USER, userId, Set.of(), null, null, event);
    }

    public static NotificationRealtimeEnvelope eventToUserExceptDevice(
            Long userId,
            String excludedDeviceId,
            RealtimeEventResponse event
    ) {
        return new NotificationRealtimeEnvelope(
                PAYLOAD_REALTIME,
                TARGET_USER_EXCEPT_DEVICE,
                userId,
                Set.of(),
                excludedDeviceId,
                null,
                event
        );
    }

    public static NotificationRealtimeEnvelope eventToRoles(Set<String> roles, RealtimeEventResponse event) {
        return new NotificationRealtimeEnvelope(PAYLOAD_REALTIME, TARGET_ROLES, null, Set.copyOf(roles), null, null, event);
    }
}
