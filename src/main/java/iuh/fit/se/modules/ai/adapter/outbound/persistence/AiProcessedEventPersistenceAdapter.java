package iuh.fit.se.modules.ai.adapter.outbound.persistence;

import iuh.fit.se.modules.ai.application.port.out.AiProcessedEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiProcessedEventPersistenceAdapter implements AiProcessedEventPort {

    private final AiProcessedEventRepository repository;

    @Override
    public int tryLockEvent(UUID eventId) {
        return repository.tryLockEvent(eventId);
    }

    @Override
    public boolean isDone(UUID eventId) {
        return repository.findById(eventId)
                .map(event -> "DONE".equals(event.getStatus()))
                .orElse(false);
    }

    @Override
    public void markAsDone(UUID eventId) {
        repository.markAsDone(eventId);
    }
}
