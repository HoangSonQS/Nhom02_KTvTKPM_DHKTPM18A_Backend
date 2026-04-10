package iuh.fit.se.modules.ai.adapter.inbound.web;

import iuh.fit.se.modules.ai.application.port.in.AiChatUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatUseCase chatUseCase;

    @PostMapping
    public ResponseEntity<String> chat(
            @RequestParam String sessionId,
            @RequestParam(required = false) Long customerId,
            @RequestBody String message) {
        String response = chatUseCase.chat(sessionId, customerId, message);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> clearHistory(@PathVariable String sessionId) {
        chatUseCase.clearHistory(sessionId);
        return ResponseEntity.noContent().build();
    }
}
