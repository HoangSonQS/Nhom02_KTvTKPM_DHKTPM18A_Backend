package iuh.fit.se.shared.audit.infrastructure.event;

import iuh.fit.se.shared.audit.application.port.out.AuditEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adapter thực thi việc phát sự kiện dùng Spring Dispatcher.
 */
@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements AuditEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
