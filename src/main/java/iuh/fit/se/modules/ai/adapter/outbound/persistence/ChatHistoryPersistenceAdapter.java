package iuh.fit.se.modules.ai.adapter.outbound.persistence;

import iuh.fit.se.modules.ai.application.port.out.ChatHistoryPersistencePort;
import iuh.fit.se.modules.ai.domain.ChatSession;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatHistoryPersistenceAdapter implements ChatHistoryPersistencePort {

    private final JpaChatSessionRepository sessionRepository;
    private final JpaChatMessageRepository messageRepository;

    @Override
    public Optional<ChatSession> findById(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    @Override
    public List<ChatMessage> findMessagesBySessionId(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Override
    public void saveSession(ChatSession session) {
        sessionRepository.save(session);
    }

    @Override
    public void saveMessage(ChatMessage message) {
        messageRepository.save(message);
    }

    @Override
    public void deleteSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}
