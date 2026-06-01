package iuh.fit.se.modules.notification.adapter.outbound.realtime;

import iuh.fit.se.modules.notification.application.port.out.NotificationRealtimeLocalPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRealtimeLocalDispatcher {

    private final List<NotificationRealtimeLocalPort> localPorts;

    public void dispatch(NotificationRealtimeEnvelope envelope) {
        if (envelope == null || envelope.target() == null) {
            return;
        }
        if (NotificationRealtimeEnvelope.PAYLOAD_NOTIFICATION.equals(envelope.payloadType())) {
            if (envelope.userId() != null && envelope.notification() != null) {
                localPorts.forEach(port -> port.publish(envelope.userId(), envelope.notification()));
            }
            return;
        }
        if (!NotificationRealtimeEnvelope.PAYLOAD_REALTIME.equals(envelope.payloadType()) || envelope.event() == null) {
            log.warn("Ignoring invalid realtime envelope with target {}", envelope.target());
            return;
        }

        switch (envelope.target()) {
            case NotificationRealtimeEnvelope.TARGET_USER -> {
                if (envelope.userId() == null) {
                    log.warn("Ignoring realtime USER envelope without userId");
                    return;
                }
                localPorts.forEach(port -> port.publishEventToUser(envelope.userId(), envelope.event()));
            }
            case NotificationRealtimeEnvelope.TARGET_USER_EXCEPT_DEVICE -> {
                if (envelope.userId() == null) {
                    log.warn("Ignoring realtime USER_EXCEPT_DEVICE envelope without userId");
                    return;
                }
                localPorts.forEach(port -> port.publishEventToUserExceptDevice(
                        envelope.userId(),
                        envelope.excludedDeviceId(),
                        envelope.event()
                ));
            }
            case NotificationRealtimeEnvelope.TARGET_ROLES -> {
                if (envelope.roles() == null || envelope.roles().isEmpty()) {
                    log.warn("Ignoring realtime ROLES envelope without roles");
                    return;
                }
                localPorts.forEach(port -> port.publishEventToRoles(envelope.roles(), envelope.event()));
            }
            default -> log.warn("Ignoring realtime envelope with unknown target {}", envelope.target());
        }
    }
}
