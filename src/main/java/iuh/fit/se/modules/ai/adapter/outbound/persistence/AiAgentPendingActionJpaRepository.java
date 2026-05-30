package iuh.fit.se.modules.ai.adapter.outbound.persistence;

import iuh.fit.se.modules.ai.domain.AiAgentPendingAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAgentPendingActionJpaRepository extends JpaRepository<AiAgentPendingAction, String> {
}
