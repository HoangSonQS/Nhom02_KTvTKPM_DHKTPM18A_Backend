package iuh.fit.se.modules.ai.application.port.in;

public interface AiChatUseCase {
    String chat(String sessionId, Long customerId, String message);
    void clearHistory(String sessionId);
}
