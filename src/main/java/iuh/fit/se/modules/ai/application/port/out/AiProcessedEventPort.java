package iuh.fit.se.modules.ai.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface AiProcessedEventPort {

    int tryLockEvent(UUID eventId);

    Optional<ProcessedEventStatus> findStatus(UUID eventId);

    void markAsDone(UUID eventId);

    record ProcessedEventStatus(UUID eventId, String status) {
    }
}
