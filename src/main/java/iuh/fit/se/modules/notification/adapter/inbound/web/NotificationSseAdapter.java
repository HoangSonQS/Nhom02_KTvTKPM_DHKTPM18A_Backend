package iuh.fit.se.modules.notification.adapter.inbound.web;

import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.port.out.NotificationRealtimeLocalPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class NotificationSseAdapter implements NotificationRealtimeLocalPort {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<Long, List<EmitterRegistration>> emittersByUser = new ConcurrentHashMap<>();
    private final Map<String, List<EmitterRegistration>> emittersByRole = new ConcurrentHashMap<>();

    public SseEmitter subscribePublic() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        EmitterRegistration registration = new EmitterRegistration(emitter, null);
        String role = "PUBLIC";
        emittersByRole.computeIfAbsent(role, ignored -> new CopyOnWriteArrayList<>()).add(registration);

        emitter.onCompletion(() -> removeRoleEmitter(role, registration));
        emitter.onTimeout(() -> removeRoleEmitter(role, registration));
        emitter.onError(error -> removeRoleEmitter(role, registration));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            removeRoleEmitter(role, registration);
        }

        return emitter;
    }

    public SseEmitter subscribe(Long userId, String role, String deviceId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        EmitterRegistration registration = new EmitterRegistration(emitter, deviceId);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(registration);
        if (role != null && !role.isBlank()) {
            emittersByRole.computeIfAbsent(role, ignored -> new CopyOnWriteArrayList<>()).add(registration);
        }

        emitter.onCompletion(() -> remove(userId, role, registration));
        emitter.onTimeout(() -> remove(userId, role, registration));
        emitter.onError(error -> remove(userId, role, registration));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(userId, role, registration);
        }

        return emitter;
    }

    @Override
    public void publish(Long userId, CustomerNotificationResponse notification) {
        List<EmitterRegistration> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (EmitterRegistration registration : emitters) {
            try {
                registration.emitter().send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException | IllegalStateException e) {
                log.debug("Removing stale notification SSE emitter for user {}", userId);
                removeFromAll(registration);
            }
        }
    }

    @Override
    public void publishEventToUser(Long userId, RealtimeEventResponse event) {
        publishRealtimeEvent(emittersByUser.get(userId), event);
    }

    @Override
    public void publishEventToUserExceptDevice(Long userId, String excludedDeviceId, RealtimeEventResponse event) {
        List<EmitterRegistration> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        publishRealtimeEvent(emitters.stream()
                .filter(registration -> excludedDeviceId == null || !excludedDeviceId.equals(registration.deviceId()))
                .toList(), event);
    }

    @Override
    public void publishEventToRoles(Set<String> roles, RealtimeEventResponse event) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        roles.forEach(role -> publishRealtimeEvent(emittersByRole.get(role), event));
    }

    private void publishRealtimeEvent(List<EmitterRegistration> emitters, RealtimeEventResponse event) {
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (EmitterRegistration registration : emitters) {
            try {
                registration.emitter().send(SseEmitter.event().name("realtime").data(event));
            } catch (IOException | IllegalStateException e) {
                removeFromAll(registration);
            }
        }
    }

    private void remove(Long userId, String role, EmitterRegistration registration) {
        List<EmitterRegistration> emitters = emittersByUser.get(userId);
        if (emitters != null) {
            emitters.remove(registration);
            if (emitters.isEmpty()) {
                emittersByUser.remove(userId);
            }
        }
        if (role != null) {
            removeRoleEmitter(role, registration);
        }
    }

    private void removeFromAll(EmitterRegistration registration) {
        emittersByUser.forEach((userId, emitters) -> {
            emitters.remove(registration);
            if (emitters.isEmpty()) {
                emittersByUser.remove(userId);
            }
        });
        emittersByRole.forEach((role, emitters) -> removeRoleEmitter(role, registration));
    }

    private void removeRoleEmitter(String role, EmitterRegistration registration) {
        List<EmitterRegistration> emitters = emittersByRole.get(role);
        if (emitters == null) {
            return;
        }
        emitters.remove(registration);
        if (emitters.isEmpty()) {
            emittersByRole.remove(role);
        }
    }

    private record EmitterRegistration(SseEmitter emitter, String deviceId) {
    }
}
