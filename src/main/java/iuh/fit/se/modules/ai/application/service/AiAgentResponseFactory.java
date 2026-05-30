package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.AiAgentUseCase;
import iuh.fit.se.modules.ai.application.port.in.AiAgentUseCase.*;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookContext;
import iuh.fit.se.modules.ai.domain.AiAgentIntent;
import iuh.fit.se.modules.ai.domain.AiAgentPendingAction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class AiAgentResponseFactory {

    public AgentResponse.AgentResponseBuilder base(String message, AiAgentAnalysis analysis) {
        return AgentResponse.builder()
                .message(message)
                .intent(analysis == null ? null : analysis.intent())
                .source(analysis == null || analysis.source() == null ? null : analysis.source().name())
                .confidence(analysis == null ? null : analysis.confidence())
                .error(null);
    }

    public AgentCard bookCard(BookContext book) {
        return AgentCard.builder()
                .type("BOOK_CARD")
                .bookId(book.id())
                .title(book.title())
                .subtitle(book.author())
                .price(book.price())
                .stock(book.quantity())
                .imageUrl(book.imageUrl())
                .url("/books/" + book.id())
                .actions(List.of(
                        AgentAction.builder()
                                .label("Xem chi ti\u1ebft")
                                .action("VIEW_BOOK_DETAIL")
                                .url("/books/" + book.id())
                                .bookId(book.id())
                                .build(),
                        AgentAction.builder()
                                .label("Th\u00eam v\u00e0o gi\u1ecf")
                                .action("ADD_TO_CART")
                                .bookId(book.id())
                                .clientAction(ClientAction.builder()
                                        .action("ADD_TO_CART")
                                        .bookId(book.id())
                                        .quantity(1)
                                        .build())
                                .build()
                ))
                .build();
    }

    public AgentCard orderCard(OrderResult order, String redirectUrl) {
        return AgentCard.builder()
                .type("ORDER_CARD")
                .orderId(order.orderId())
                .title("\u0110\u01a1n h\u00e0ng #" + order.orderId())
                .subtitle(order.fulfillmentStatus())
                .price(order.finalAmount())
                .url("/orders/" + order.orderId())
                .actions(redirectUrl == null ? List.of(
                        AgentAction.builder()
                                .label("Xem chi ti\u1ebft \u0111\u01a1n")
                                .action("VIEW_ORDER")
                                .url("/orders/" + order.orderId())
                                .orderId(order.orderId())
                                .build()
                ) : List.of(
                        AgentAction.builder()
                                .label("Thanh to\u00e1n VNPAY")
                                .action("PAY_ORDER")
                                .url(redirectUrl)
                                .orderId(order.orderId())
                                .build(),
                        AgentAction.builder()
                                .label("Xem chi ti\u1ebft \u0111\u01a1n")
                                .action("VIEW_ORDER")
                                .url("/orders/" + order.orderId())
                                .orderId(order.orderId())
                                .build()
                ))
                .build();
    }

    public PendingActionResult pending(AiAgentPendingAction action, AiAgentService.ActionPayload payload, BigDecimal estimatedTotal) {
        return PendingActionResult.builder()
                .pendingActionId(action.getId())
                .intent(action.getIntent())
                .expiresAt(action.getExpiresAt())
                .summary(Map.of(
                        "bookTitle", payload.bookTitle() == null ? "" : payload.bookTitle(),
                        "quantity", payload.quantity(),
                        "estimatedTotal", estimatedTotal == null ? BigDecimal.ZERO : estimatedTotal
                ))
                .build();
    }

    public List<AgentAction> pendingActions(AiAgentPendingAction action) {
        return List.of(
                AgentAction.builder()
                        .label("X\u00e1c nh\u1eadn")
                        .action("CONFIRM_PENDING_ACTION")
                        .pendingActionId(action.getId())
                        .build(),
                AgentAction.builder()
                        .label("H\u1ee7y")
                        .action("CANCEL_PENDING_ACTION")
                        .pendingActionId(action.getId())
                        .build()
        );
    }

    public AgentError error(String code, String message) {
        return AgentError.builder().code(code).message(message).build();
    }

    public List<String> defaultSuggestions() {
        return List.of("T\u00ecm s\u00e1ch hay", "Xem gi\u1ecf h\u00e0ng", "\u0110\u01a1n h\u00e0ng g\u1ea7n nh\u1ea5t");
    }

    public ConfirmationCard confirmationCard(AiAgentPendingAction action, AiAgentService.ActionPayload payload) {
        String title = action.getIntent().normalized() == AiAgentIntent.PLACE_ORDER
                ? "X\u00e1c nh\u1eadn \u0111\u1eb7t h\u00e0ng"
                : "X\u00e1c nh\u1eadn thao t\u00e1c";
        String description = action.getIntent().normalized() == AiAgentIntent.PLACE_ORDER
                ? "\u0110\u1eb7t " + payload.quantity() + " cu\u1ed1n " + displayBookName(payload) + " b\u1eb1ng " + payload.paymentMethod() + "."
                : "B\u1ea1n x\u00e1c nh\u1eadn th\u1ef1c hi\u1ec7n thao t\u00e1c n\u00e0y?";
        if (action.getIntent().normalized() == AiAgentIntent.CANCEL_ORDER) {
            title = "X\u00e1c nh\u1eadn h\u1ee7y \u0111\u01a1n h\u00e0ng";
            description = "H\u1ee7y \u0111\u01a1n h\u00e0ng #" + payload.orderId() + ".";
        }
        if (action.getIntent().normalized() == AiAgentIntent.PLACE_ORDER
                && payload.bookId() == null
                && (payload.bookTitle() == null || payload.bookTitle().isBlank())) {
            description = "\u0110\u1eb7t c\u00e1c s\u00e1ch trong gi\u1ecf h\u00e0ng b\u1eb1ng " + payload.paymentMethod() + ".";
        }
        return ConfirmationCard.builder()
                .pendingActionId(action.getId())
                .title(title)
                .description(description)
                .confirmText("X\u00e1c nh\u1eadn")
                .cancelText("H\u1ee7y")
                .build();
    }

    private String displayBookName(AiAgentService.ActionPayload payload) {
        return payload.bookTitle() == null || payload.bookTitle().isBlank()
                ? "s\u00e1ch ID " + payload.bookId()
                : "\"" + payload.bookTitle() + "\"";
    }
}
