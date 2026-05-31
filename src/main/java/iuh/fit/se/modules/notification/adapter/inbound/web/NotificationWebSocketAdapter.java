package iuh.fit.se.modules.notification.adapter.inbound.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.notification.application.port.in.CustomerNotificationResponse;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketAdapter extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<Long, List<SocketRegistration>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, List<SocketRegistration>> sessionsByRole = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        String role = (String) session.getAttributes().get("role");
        String deviceId = (String) session.getAttributes().get("deviceId");
        if (userId == null) {
            closeQuietly(session);
            return;
        }

        SocketRegistration registration = new SocketRegistration(session, deviceId);
        sessionsByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(registration);
        if (role != null && !role.isBlank()) {
            sessionsByRole.computeIfAbsent(role, ignored -> new CopyOnWriteArrayList<>()).add(registration);
        }

        send(registration, "connected", "ok");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Notification WebSocket transport error: {}", exception.getMessage());
        removeSession(session);
        closeQuietly(session);
    }

    public void publish(Long userId, CustomerNotificationResponse notification) {
        publish(sessionsByUser.get(userId), "notification", notification);
    }

    public void publishEventToUser(Long userId, RealtimeEventResponse event) {
        publish(sessionsByUser.get(userId), "realtime", event);
    }

    public void publishEventToUserExceptDevice(Long userId, String excludedDeviceId, RealtimeEventResponse event) {
        List<SocketRegistration> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        publish(sessions.stream()
                .filter(registration -> excludedDeviceId == null || !excludedDeviceId.equals(registration.deviceId()))
                .toList(), "realtime", event);
    }

    public void publishEventToRoles(Set<String> roles, RealtimeEventResponse event) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        roles.forEach(role -> publish(sessionsByRole.get(role), "realtime", event));
    }

    private void publish(List<SocketRegistration> sessions, String eventName, Object data) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        sessions.forEach(session -> send(session, eventName, data));
    }

    private void send(SocketRegistration registration, String eventName, Object data) {
        WebSocketSession session = registration.session();
        if (!session.isOpen()) {
            removeFromAll(registration);
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of("event", eventName, "data", data));
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize realtime WebSocket payload: {}", e.getMessage());
        } catch (IOException | IllegalStateException e) {
            removeFromAll(registration);
            closeQuietly(session);
        }
    }

    private void removeSession(WebSocketSession session) {
        sessionsByUser.forEach((userId, sessions) -> {
            sessions.removeIf(registration -> registration.session().getId().equals(session.getId()));
            if (sessions.isEmpty()) {
                sessionsByUser.remove(userId);
            }
        });
        sessionsByRole.forEach((role, sessions) -> {
            sessions.removeIf(registration -> registration.session().getId().equals(session.getId()));
            if (sessions.isEmpty()) {
                sessionsByRole.remove(role);
            }
        });
    }

    private void removeFromAll(SocketRegistration registration) {
        sessionsByUser.forEach((userId, sessions) -> {
            sessions.remove(registration);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(userId);
            }
        });
        sessionsByRole.forEach((role, sessions) -> {
            sessions.remove(registration);
            if (sessions.isEmpty()) {
                sessionsByRole.remove(role);
            }
        });
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) {
        }
    }

    private record SocketRegistration(WebSocketSession session, String deviceId) {
    }
}
