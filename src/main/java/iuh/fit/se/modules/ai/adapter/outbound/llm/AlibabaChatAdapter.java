package iuh.fit.se.modules.ai.adapter.outbound.llm;

import iuh.fit.se.modules.ai.application.port.out.LlmPort;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import iuh.fit.se.modules.ai.domain.ChatRole;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AlibabaChatAdapter implements LlmPort {

    private final ChatModel chatModel;
    private static final String CHAT_MODEL = "qwen3.5-122b-a10b";

    @Override
    public String chat(String prompt, List<ChatMessage> history) {
        List<Message> messages = new ArrayList<>();
        
        // Add System instruction
        messages.add(new SystemMessage("You are SEBook Assistant, a helpful book store agent. " +
                "You provide book recommendations and answer customer questions about our inventory."));

        // Add history
        for (ChatMessage msg : history) {
            if (msg.getRole() == ChatRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == ChatRole.ASSISTANT) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        // Add latest prompt
        messages.add(new UserMessage(prompt));

        ChatResponse response = chatModel.call(new Prompt(messages, 
            DashScopeChatOptions.builder().withModel(CHAT_MODEL).build()));

        return response.getResult().getOutput().getText();
    }
}
