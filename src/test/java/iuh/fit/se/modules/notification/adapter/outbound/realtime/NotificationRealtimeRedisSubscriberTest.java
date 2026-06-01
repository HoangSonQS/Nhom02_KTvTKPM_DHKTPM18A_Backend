package iuh.fit.se.modules.notification.adapter.outbound.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.Message;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRealtimeRedisSubscriberTest {

    @Test
    void whenRedisEnvelopeArrives_thenDeserializeAndDispatchLocally() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        NotificationRealtimeLocalDispatcher localDispatcher = mock(NotificationRealtimeLocalDispatcher.class);
        NotificationRealtimeRedisSubscriber subscriber =
                new NotificationRealtimeRedisSubscriber(objectMapper, localDispatcher);
        RealtimeEventResponse event = new RealtimeEventResponse(
                "STOCKTAKE_UPDATED",
                null,
                null,
                null,
                null,
                null,
                null,
                42L,
                null,
                null,
                "APPROVED",
                "Stocktake approved",
                LocalDateTime.now()
        );
        NotificationRealtimeEnvelope envelope =
                NotificationRealtimeEnvelope.eventToRoles(Set.of("ADMIN", "STAFF_WAREHOUSE"), event);
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(objectMapper.writeValueAsBytes(envelope));

        subscriber.onMessage(message, null);

        ArgumentCaptor<NotificationRealtimeEnvelope> captor =
                ArgumentCaptor.forClass(NotificationRealtimeEnvelope.class);
        verify(localDispatcher).dispatch(captor.capture());
        assertThat(captor.getValue().target()).isEqualTo(NotificationRealtimeEnvelope.TARGET_ROLES);
        assertThat(captor.getValue().roles()).containsExactlyInAnyOrder("ADMIN", "STAFF_WAREHOUSE");
        assertThat(captor.getValue().event().stocktakeId()).isEqualTo(42L);
    }
}
