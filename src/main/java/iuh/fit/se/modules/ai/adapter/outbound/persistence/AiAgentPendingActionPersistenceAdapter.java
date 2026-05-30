package iuh.fit.se.modules.ai.adapter.outbound.persistence;

import iuh.fit.se.modules.ai.application.port.out.AiAgentPendingActionPersistencePort;
import iuh.fit.se.modules.ai.domain.AiAgentPendingAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AiAgentPendingActionPersistenceAdapter implements AiAgentPendingActionPersistencePort {

    private final AiAgentPendingActionJpaRepository repository;

    @Override
    public AiAgentPendingAction save(AiAgentPendingAction action) {
        return repository.save(action);
    }

    @Override
    public Optional<AiAgentPendingAction> findById(String id) {
        return repository.findById(id);
    }
}
