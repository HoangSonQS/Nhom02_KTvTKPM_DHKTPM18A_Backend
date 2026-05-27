package iuh.fit.se.modules.notification.adapter.inbound.web;

import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.port.out.NotificationRealtimePort;
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
public class NotificationSseAdapter implements NotificationRealtimePort {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> emittersByRole = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId, String role) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        if (role != null && !role.isBlank()) {
            emittersByRole.computeIfAbsent(role, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        }

        emitter.onCompletion(() -> remove(userId, role, emitter));
        emitter.onTimeout(() -> remove(userId, role, emitter));
        emitter.onError(error -> remove(userId, role, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove(userId, role, emitter);
        }

        return emitter;
    }

    @Override
    public void publish(Long userId, CustomerNotificationResponse notification) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException | IllegalStateException e) {
                log.debug("Removing stale notification SSE emitter for user {}", userId);
                removeFromAll(emitter);
            }
        }
    }

    @Override
    public void publishEventToUser(Long userId, RealtimeEventResponse event) {
        publishRealtimeEvent(emittersByUser.get(userId), event);
    }

    @Override
    public void publishEventToRoles(Set<String> roles, RealtimeEventResponse event) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        roles.forEach(role -> publishRealtimeEvent(emittersByRole.get(role), event));
    }

    private void publishRealtimeEvent(List<SseEmitter> emitters, RealtimeEventResponse event) {
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("realtime").data(event));
            } catch (IOException | IllegalStateException e) {
                removeFromAll(emitter);
            }
        }
    }

    private void remove(Long userId, String role, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByUser.remove(userId);
            }
        }
        if (role != null) {
            removeRoleEmitter(role, emitter);
        }
    }

    private void removeFromAll(SseEmitter emitter) {
        emittersByUser.forEach((userId, emitters) -> {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByUser.remove(userId);
            }
        });
        emittersByRole.forEach((role, emitters) -> removeRoleEmitter(role, emitter));
    }

    private void removeRoleEmitter(String role, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByRole.get(role);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByRole.remove(role);
        }
    }
}
