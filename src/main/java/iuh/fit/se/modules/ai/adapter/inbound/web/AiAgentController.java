package iuh.fit.se.modules.ai.adapter.inbound.web;

import iuh.fit.se.modules.ai.application.port.in.AiAgentUseCase;
import iuh.fit.se.shared.api.ApiResponse;
import iuh.fit.se.shared.config.UserPrincipal;
import iuh.fit.se.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/agent")
@RequiredArgsConstructor
public class AiAgentController {

    private final AiAgentUseCase agentUseCase;

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<AiAgentUseCase.AgentResponse>> message(@RequestBody AgentMessageRequest request) {
        AiAgentUseCase.AgentResponse response = agentUseCase.handleMessage(
                new AiAgentUseCase.AgentMessageCommand(
                        request.sessionId(),
                        currentUserIdOrNull(),
                        request.message(),
                        request.clientAction()
                )
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/actions/{pendingActionId}/confirm")
    public ResponseEntity<ApiResponse<AiAgentUseCase.AgentResponse>> confirm(
            @PathVariable String pendingActionId,
            HttpServletRequest request
    ) {
        AiAgentUseCase.AgentResponse response = agentUseCase.confirmAction(
                pendingActionId,
                SecurityUtils.getCurrentUserId(),
                clientIp(request)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/actions/{pendingActionId}/cancel")
    public ResponseEntity<ApiResponse<AiAgentUseCase.AgentResponse>> cancel(@PathVariable String pendingActionId) {
        AiAgentUseCase.AgentResponse response = agentUseCase.cancelAction(
                pendingActionId,
                SecurityUtils.getCurrentUserId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    record AgentMessageRequest(String sessionId, String message, AiAgentUseCase.ClientAction clientAction) {
    }
}
