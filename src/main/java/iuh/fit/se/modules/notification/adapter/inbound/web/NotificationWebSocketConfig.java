package iuh.fit.se.modules.notification.adapter.inbound.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class NotificationWebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketAdapter notificationWebSocketAdapter;
    private final NotificationWebSocketAuthInterceptor notificationWebSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketAdapter, "/api/v1/notifications/ws")
                .addInterceptors(notificationWebSocketAuthInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
