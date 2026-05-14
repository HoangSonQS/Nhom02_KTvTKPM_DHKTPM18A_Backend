package iuh.fit.se.shared.audit.aspect;

import iuh.fit.se.shared.audit.annotation.Auditable;
import iuh.fit.se.shared.audit.application.port.out.AuditEventPublisherPort;
import iuh.fit.se.shared.audit.domain.event.UserActionAuditedEvent;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private AuditEventPublisherPort auditEventPublisherPort;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenCollectionResult_whenAuditAction_thenNewValueIsSummaryOnly() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "seller@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STAFF_SELLER"))));

        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"orders"});

        AuditLogAspect aspect = new AuditLogAspect(auditEventPublisherPort);

        aspect.auditAction(joinPoint, auditable("STAFF_UPDATE_ORDER_STATUS"), List.of("item-1", "item-2"));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditEventPublisherPort).publish(eventCaptor.capture());
        UserActionAuditedEvent event = (UserActionAuditedEvent) eventCaptor.getValue();

        assertEquals("STAFF_SELLER", event.role());
        assertEquals("orders", event.target());
        assertEquals("Thao tác thành công, số bản ghi: 2", event.newValue());
        assertFalse(event.newValue().contains("["));
    }

    private Auditable auditable(String action) {
        return new Auditable() {
            @Override
            public String action() {
                return action;
            }

            @Override
            public Class<Auditable> annotationType() {
                return Auditable.class;
            }
        };
    }
}
