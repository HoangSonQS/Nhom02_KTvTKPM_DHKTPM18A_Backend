package iuh.fit.se.modules.notification.adapter.outbound.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Slf4j
public class NotificationRealtimeRedisConfig {

    public static final String CHANNEL = "notification:realtime:v1";

    @Bean
    public RedisMessageListenerContainer notificationRealtimeRedisContainer(
            RedisConnectionFactory connectionFactory,
            NotificationRealtimeRedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer() {
            @Override
            public void start() {
                try {
                    super.start();
                } catch (RuntimeException exception) {
                    log.warn("Could not start realtime Redis subscriber; continuing with local fallback while retrying: {}",
                            exception.getMessage());
                }
            }
        };
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(CHANNEL));
        return container;
    }
}
