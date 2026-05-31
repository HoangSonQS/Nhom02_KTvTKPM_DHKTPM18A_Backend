package iuh.fit.se.modules.ai.application.port.out;

import java.util.Optional;

public interface AiCheckoutDraftPersistencePort {

    Optional<CheckoutDraft> findBySessionId(String sessionId);

    void save(String sessionId, CheckoutDraft draft);

    void delete(String sessionId);

    record CheckoutDraft(Long bookId, String bookTitle, int quantity) {
    }
}
