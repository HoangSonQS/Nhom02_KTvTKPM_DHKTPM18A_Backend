package iuh.fit.se.modules.ai.application.port.out;

import java.util.Optional;
import java.util.Set;

public interface AiSearchContextPersistencePort {

    Optional<SearchContext> findBySessionId(String sessionId);

    void save(String sessionId, SearchContext context);

    record SearchContext(String query, Set<Long> shownBookIds) {
    }
}
