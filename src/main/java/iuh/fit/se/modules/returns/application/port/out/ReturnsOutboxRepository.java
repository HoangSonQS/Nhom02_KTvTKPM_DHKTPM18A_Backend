package iuh.fit.se.modules.returns.application.port.out;

public interface ReturnsOutboxRepository {
    void saveEvent(Object event);
}
