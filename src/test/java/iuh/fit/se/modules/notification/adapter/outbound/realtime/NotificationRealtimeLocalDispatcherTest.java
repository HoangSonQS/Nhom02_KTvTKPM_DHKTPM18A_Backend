package iuh.fit.se.modules.notification.adapter.outbound.realtime;

import iuh.fit.se.modules.notification.application.port.in.RealtimeEventResponse;
import iuh.fit.se.modules.notification.application.port.out.NotificationRealtimeLocalPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationRealtimeLocalDispatcherTest {

    @Test
    void whenRoleEnvelopeArrives_thenDispatchToBothWebSocketAndSseRegistries() {
        NotificationRealtimeLocalPort sseAdapter = mock(NotificationRealtimeLocalPort.class);
        NotificationRealtimeLocalPort webSocketAdapter = mock(NotificationRealtimeLocalPort.class);
        NotificationRealtimeLocalDispatcher dispatcher =
                new NotificationRealtimeLocalDispatcher(List.of(sseAdapter, webSocketAdapter));
        RealtimeEventResponse event = new RealtimeEventResponse(
                "BOOK_CHANGED",
                null,
                null,
                7L,
                null,
                null,
                null,
                null,
                null,
                null,
                "BOOK",
                "Book changed",
                LocalDateTime.now()
        );
        Set<String> roles = Set.of("ADMIN", "PUBLIC");

        dispatcher.dispatch(NotificationRealtimeEnvelope.eventToRoles(roles, event));

        verify(sseAdapter).publishEventToRoles(roles, event);
        verify(webSocketAdapter).publishEventToRoles(roles, event);
    }
}
