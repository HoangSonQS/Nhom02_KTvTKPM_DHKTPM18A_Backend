package iuh.fit.se.modules.ai.application.port.in;

public interface EmbeddingSyncUseCase {
    void syncBook(Long bookId);
    void syncAllBooks();
}
