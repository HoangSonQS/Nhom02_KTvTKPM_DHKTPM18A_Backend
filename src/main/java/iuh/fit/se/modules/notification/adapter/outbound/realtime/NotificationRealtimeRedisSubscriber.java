package iuh.fit.se.modules.notification.adapter.outbound.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRealtimeRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationRealtimeLocalDispatcher localDispatcher;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationRealtimeEnvelope envelope = objectMapper.readValue(message.getBody(), NotificationRealtimeEnvelope.class);
            localDispatcher.dispatch(envelope);
        } catch (Exception exception) {
            log.warn("Could not consume notification realtime Redis message: {}", exception.getMessage());
        }
    }
}
