package iuh.fit.se.modules.ai.application.port.in;

import iuh.fit.se.modules.ai.domain.AiAgentIntent;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AiAgentUseCase {
    AgentResponse handleMessage(AgentMessageCommand command);
    AgentResponse confirmAction(String pendingActionId, Long userId, String requesterIpAddress);
    AgentResponse cancelAction(String pendingActionId, Long userId);

    record AgentMessageCommand(String sessionId, Long userId, String message, ClientAction clientAction) {
        public AgentMessageCommand(String sessionId, Long userId, String message) {
            this(sessionId, userId, message, null);
        }
    }

    @Builder
    record AgentResponse(
            String message,
            AiAgentIntent intent,
            String source,
            Double confidence,
            List<AgentCard> cards,
            List<AgentAction> actions,
            PendingActionResult pendingAction,
            AgentError error,
            List<String> suggestions,
            ConfirmationCard confirmationCard,
            String redirectUrl,
            List<BookResult> books,
            CartResult cart,
            OrderResult order
    ) {
    }

    @Builder
    record ConfirmationCard(
            String pendingActionId,
            String title,
            String description,
            String confirmText,
            String cancelText
    ) {
    }

    @Builder
    record AgentCard(
            String type,
            Long bookId,
            Long orderId,
            Long addressId,
            String title,
            String subtitle,
            String message,
            BigDecimal price,
            Integer stock,
            String imageUrl,
            String url,
            List<AgentAction> actions,
            Map<String, Object> metadata
    ) {
    }

    @Builder
    record AgentAction(
            String label,
            String action,
            String pendingActionId,
            String url,
            Long bookId,
            Long orderId,
            Long addressId,
            String message,
            ClientAction clientAction
    ) {
    }

    @Builder
    record PendingActionResult(
            String pendingActionId,
            AiAgentIntent intent,
            LocalDateTime expiresAt,
            Map<String, Object> summary
    ) {
    }

    @Builder
    record AgentError(
            String code,
            String message
    ) {
    }

    @Builder
    record ClientAction(
            String action,
            Long bookId,
            Long orderId,
            Long addressId,
            Integer quantity,
            String paymentMethod,
            String shippingAddress,
            String customerPhone,
            String message
    ) {
    }

    @Builder
    record BookResult(
            Long bookId,
            String title,
            String author,
            BigDecimal price,
            int quantity,
            Integer requestedQuantity,
            String imageUrl,
            String description
    ) {
    }

    @Builder
    record CartResult(
            BigDecimal totalAmount,
            List<CartItemResult> items
    ) {
    }

    @Builder
    record CartItemResult(
            Long bookId,
            String title,
            BigDecimal price,
            int quantity
    ) {
    }

    @Builder
    record OrderResult(
            Long orderId,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String fulfillmentStatus,
            String paymentMethod
    ) {
    }
}
