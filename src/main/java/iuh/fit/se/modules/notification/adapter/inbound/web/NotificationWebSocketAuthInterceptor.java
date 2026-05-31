package iuh.fit.se.modules.notification.adapter.inbound.web;

import iuh.fit.se.shared.config.JwtTokenProvider;
import iuh.fit.se.shared.security.AccessTokenSessionValidator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectProvider<AccessTokenSessionValidator> accessTokenSessionValidatorProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null || token.isBlank() || !jwtTokenProvider.isTokenValid(token)) {
            return false;
        }

        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            Long userId = readLongClaim(claims.get("userId"));
            String role = claims.get("role", String.class);
            String deviceId = claims.get("deviceId", String.class);
            Integer refreshVersion = claims.get("rv", Integer.class);
            if (userId == null) {
                return false;
            }

            AccessTokenSessionValidator validator = accessTokenSessionValidatorProvider.getIfAvailable();
            if (validator != null && !validator.isActive(userId, deviceId, refreshVersion)) {
                return false;
            }

            attributes.put("userId", userId);
            attributes.put("role", role);
            attributes.put("deviceId", deviceId);
            return true;
        } catch (Exception e) {
            log.debug("Rejected notification WebSocket handshake: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private Long readLongClaim(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
