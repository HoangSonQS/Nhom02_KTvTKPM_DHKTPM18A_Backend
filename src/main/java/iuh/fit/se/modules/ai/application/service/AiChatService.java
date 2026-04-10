package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.AiChatUseCase;
import iuh.fit.se.modules.ai.application.port.out.ChatHistoryPersistencePort;
import iuh.fit.se.modules.ai.application.port.out.LlmPort;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import iuh.fit.se.modules.ai.domain.ChatRole;
import iuh.fit.se.modules.ai.domain.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatService implements AiChatUseCase {

    private final LlmPort llmPort;
    private final ChatHistoryPersistencePort historyPort;

    @Override
    @Transactional
    public String chat(String sessionId, Long customerId, String message) {
        ChatSession session = historyPort.findById(sessionId)
                .orElseGet(() -> {
                    ChatSession newSession = ChatSession.create(sessionId, customerId);
                    historyPort.saveSession(newSession);
                    return newSession;
                });

        // 1. Save user message
        ChatMessage userMsg = ChatMessage.create(sessionId, ChatRole.USER, message);
        session.addMessage(userMsg);
        historyPort.saveMessage(userMsg);

        // 2. Call LLM with history
        String response = llmPort.chat(message, session.getMessages());

        // 3. Save assistant response
        ChatMessage assistantMsg = ChatMessage.create(sessionId, ChatRole.ASSISTANT, response);
        session.addMessage(assistantMsg);
        historyPort.saveMessage(assistantMsg);

        return response;
    }

    @Override
    @Transactional
    public void clearHistory(String sessionId) {
        historyPort.deleteSession(sessionId);
    }
}
