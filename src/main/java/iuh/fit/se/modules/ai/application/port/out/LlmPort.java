package iuh.fit.se.modules.ai.application.port.out;

import java.util.List;
import iuh.fit.se.modules.ai.domain.ChatMessage;

public interface LlmPort {
    String chat(String prompt, List<ChatMessage> history);
}
