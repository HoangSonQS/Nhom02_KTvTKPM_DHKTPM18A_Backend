package iuh.fit.se.modules.ai.application.port.out;

import java.util.List;
import java.util.Optional;

public interface AiBookContextPersistencePort {

    Optional<BookContext> findBySessionId(String sessionId);

    void save(String sessionId, BookContext context);

    record BookContext(List<BookReference> books) {
    }

    record BookReference(Long id, String title) {
    }
}
