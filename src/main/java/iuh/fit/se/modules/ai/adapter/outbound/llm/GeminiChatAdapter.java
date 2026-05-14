package iuh.fit.se.modules.ai.adapter.outbound.llm;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentResponse;
import iuh.fit.se.modules.ai.application.port.out.LlmPort;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import iuh.fit.se.modules.ai.domain.ChatRole;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class GeminiChatAdapter implements LlmPort {

    private final String model;
    private final List<Client> clients;
    private final AtomicInteger cursor = new AtomicInteger();

    public GeminiChatAdapter(
            @Value("${app.ai.gemini.chat.model:gemini-2.0-flash}") String model,
            @Value("${app.ai.gemini.chat.api-keys:}") String apiKeys
    ) {
        this.model = model;
        this.clients = parseApiKeys(apiKeys).stream()
                .map(apiKey -> Client.builder().apiKey(apiKey).build())
                .toList();
    }

    @Override
    public String chat(String prompt, List<ChatMessage> history) {
        if (clients.isEmpty()) {
            throw new IllegalStateException("No Gemini API key configured");
        }

        String content = buildContent(prompt, history);
        int start = Math.floorMod(cursor.getAndIncrement(), clients.size());
        RuntimeException lastFailure = null;

        for (int offset = 0; offset < clients.size(); offset++) {
            int index = (start + offset) % clients.size();
            try {
                GenerateContentResponse response = clients.get(index).models.generateContent(model, content, null);
                String text = response.text();
                if (text == null || text.isBlank()) {
                    throw new IllegalStateException("Gemini returned an empty response");
                }
                return text;
            } catch (RuntimeException e) {
                lastFailure = e;
                if (isQuotaError(e)) {
                    log.warn("Gemini API key #{} exceeded quota, rotating to the next key", index + 1);
                    continue;
                }
                throw e;
            }
        }

        throw lastFailure != null ? lastFailure : new IllegalStateException("Gemini chat failed");
    }

    private String buildContent(String prompt, List<ChatMessage> history) {
        StringBuilder content = new StringBuilder();
        content.append("Bạn là SEBook Assistant, trợ lý bán sách của nhà sách SEBook. ");
        content.append("Hãy trả lời bằng tiếng Việt có dấu, tự nhiên, lịch sự và dựa trên dữ liệu kho sách được cung cấp.\n\n");

        if (history != null && !history.isEmpty()) {
            content.append("[LỊCH SỬ HỘI THOẠI]\n");
            for (ChatMessage msg : history) {
                if (msg.getRole() == ChatRole.USER) {
                    content.append("Khách hàng: ");
                } else if (msg.getRole() == ChatRole.ASSISTANT) {
                    content.append("Trợ lý: ");
                } else {
                    continue;
                }
                content.append(msg.getContent()).append("\n");
            }
            content.append("\n");
        }

        content.append("[YÊU CẦU HIỆN TẠI]\n").append(prompt);
        return content.toString();
    }

    private List<String> parseApiKeys(String apiKeys) {
        if (apiKeys == null || apiKeys.isBlank()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(Arrays.stream(apiKeys.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList()));
    }

    private boolean isQuotaError(RuntimeException e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ApiException apiException && apiException.code() == 429) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("429")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @PreDestroy
    void closeClients() {
        clients.forEach(Client::close);
    }
}
