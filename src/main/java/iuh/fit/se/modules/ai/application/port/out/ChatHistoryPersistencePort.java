package iuh.fit.se.modules.ai.application.port.out;

import iuh.fit.se.modules.ai.domain.ChatSession;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import java.util.List;
import java.util.Optional;

public interface ChatHistoryPersistencePort {
    Optional<ChatSession> findById(String sessionId);
    List<ChatMessage> findMessagesBySessionId(String sessionId);
    void saveSession(ChatSession session);
    void saveMessage(ChatMessage message);
    void deleteSession(String sessionId);
}
