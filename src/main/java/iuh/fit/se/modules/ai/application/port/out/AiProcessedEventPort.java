package iuh.fit.se.modules.ai.application.port.out;

import java.util.UUID;

public interface AiProcessedEventPort {

    int tryLockEvent(UUID eventId);

    boolean isDone(UUID eventId);

    void markAsDone(UUID eventId);
}
