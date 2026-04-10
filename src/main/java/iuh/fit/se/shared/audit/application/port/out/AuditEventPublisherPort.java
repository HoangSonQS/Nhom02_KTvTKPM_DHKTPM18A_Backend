package iuh.fit.se.shared.audit.application.port.out;

/**
 * Port để phát các sự kiện Audit.
 */
public interface AuditEventPublisherPort {
    void publish(Object event);
}
