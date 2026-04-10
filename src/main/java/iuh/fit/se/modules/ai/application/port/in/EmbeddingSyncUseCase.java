package iuh.fit.se.modules.ai.application.port.in;

import java.util.UUID;

public interface EmbeddingSyncUseCase {
    void syncBook(Long bookId, UUID eventId);
    void syncDeletion(Long bookId);
    void syncAllBooks();
}
