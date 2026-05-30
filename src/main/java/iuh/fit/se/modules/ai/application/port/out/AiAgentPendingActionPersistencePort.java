package iuh.fit.se.modules.ai.application.port.out;

import iuh.fit.se.modules.ai.domain.AiAgentPendingAction;

import java.util.Optional;

public interface AiAgentPendingActionPersistencePort {
    AiAgentPendingAction save(AiAgentPendingAction action);
    Optional<AiAgentPendingAction> findById(String id);
}
