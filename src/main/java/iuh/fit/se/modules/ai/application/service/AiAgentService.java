package iuh.fit.se.modules.ai.application.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.modules.ai.application.port.in.AiAgentUseCase;
import iuh.fit.se.modules.ai.application.port.in.AiAgentUseCase.*;
import iuh.fit.se.modules.ai.application.port.out.*;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookContext;
import iuh.fit.se.modules.ai.domain.*;
import iuh.fit.se.modules.cart.application.port.in.CartInternalUseCase;
import iuh.fit.se.modules.order.application.port.in.OrderInternalUseCase;
import iuh.fit.se.modules.payment.application.port.in.PaymentUseCase;
import iuh.fit.se.shared.exception.AppException;
import iuh.fit.se.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAgentService implements AiAgentUseCase {

    private static final double RULE_THRESHOLD = 0.85;
    private static final double MIN_CONFIDENCE = 0.65;
    private static final int RAG_TOP_K = 5;
    private static final int PENDING_EXPIRY_MINUTES = 10;

    private final LlmPort llmPort;
    private final ChatHistoryPersistencePort historyPort;
    private final VectorStorePort vectorStorePort;
    private final CatalogBookPort catalogBookPort;
    private final CartInternalUseCase cartUseCase;
    private final OrderInternalUseCase orderUseCase;
    private final PaymentUseCase paymentUseCase;
    private final AiAgentPendingActionPersistencePort pendingActionPort;
    private final ObjectMapper objectMapper;
    private final AiAgentRuleEngine ruleEngine;
    private final GeminiIntentParser geminiIntentParser;
    private final AiAgentValidator validator;
    private final AiAgentResponseFactory responseFactory;
    private final Map<String, List<BookReference>> sessionBookContext = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public AgentResponse handleMessage(AgentMessageCommand command) {
        String message = effectiveMessage(command);
        saveMessage(command.sessionId(), command.userId(), ChatRole.USER, message);
        AgentResponse response = routeMessage(command, message);
        rememberBookContext(command.sessionId(), response);
        saveMessage(command.sessionId(), command.userId(), ChatRole.ASSISTANT, response.message());
        return response;
    }

    @Override
    @Transactional
    public AgentResponse confirmAction(String pendingActionId, Long userId, String requesterIpAddress) {
        AiAgentPendingAction action = pendingActionPort.findById(pendingActionId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Kh\u00f4ng t\u00ecm th\u1ea5y thao t\u00e1c c\u1ea7n x\u00e1c nh\u1eadn"));
        if (action.getExpiresAt().isBefore(LocalDateTime.now())) {
            action.markExpired();
            pendingActionPort.save(action);
            return AgentResponse.builder()
                    .message("Thao t\u00e1c x\u00e1c nh\u1eadn \u0111\u00e3 h\u1ebft h\u1ea1n. B\u1ea1n vui l\u00f2ng t\u1ea1o l\u1ea1i y\u00eau c\u1ea7u nh\u00e9.")
                    .intent(action.getIntent())
                    .source(AiAgentSource.FALLBACK.name())
                    .error(responseFactory.error("PENDING_EXPIRED", "Thao t\u00e1c \u0111\u00e3 h\u1ebft h\u1ea1n"))
                    .suggestions(responseFactory.defaultSuggestions())
                    .build();
        }
        action.ensureUsableBy(userId);

        ActionPayload payload = readPayload(action.getPayload());
        AgentResponse response;
        try {
            response = switch (action.getIntent().normalized()) {
                case PLACE_ORDER -> executePlaceOrder(userId, payload, requesterIpAddress);
                case CANCEL_ORDER -> executeCancelOrder(userId, payload);
                case PAY_ORDER -> executePayOrder(userId, payload, requesterIpAddress);
                case CHANGE_SHIPPING_ADDRESS -> unsupportedImportantAction(action.getIntent());
                // Legacy pending actions are still accepted.
                case ADD_TO_CART -> executeAddToCart(userId, payload);
                case UPDATE_CART_QUANTITY -> executeUpdateCart(userId, payload);
                case REMOVE_FROM_CART -> executeRemoveCart(userId, payload);
                default -> throw new AppException(ErrorCode.INVALID_INPUT, "Thao t\u00e1c n\u00e0y kh\u00f4ng c\u1ea7n x\u00e1c nh\u1eadn");
            };
            if (response.error() == null) {
                action.confirm();
            } else {
                action.markFailed();
            }
        } catch (AppException e) {
            action.markFailed();
            throw e;
        } finally {
            pendingActionPort.save(action);
        }
        saveMessage(action.getSessionId(), userId, ChatRole.ASSISTANT, response.message());
        return response;
    }

    @Override
    @Transactional
    public AgentResponse cancelAction(String pendingActionId, Long userId) {
        AiAgentPendingAction action = pendingActionPort.findById(pendingActionId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Kh\u00f4ng t\u00ecm th\u1ea5y thao t\u00e1c c\u1ea7n h\u1ee7y"));
        action.cancel(userId);
        pendingActionPort.save(action);
        return AgentResponse.builder()
                .message("M\u00ecnh \u0111\u00e3 h\u1ee7y thao t\u00e1c n\u00e0y. B\u1ea1n c\u00f3 th\u1ec3 ti\u1ebfp t\u1ee5c h\u1ecfi ho\u1eb7c ch\u1ecdn s\u00e1ch kh\u00e1c nh\u00e9.")
                .intent(action.getIntent())
                .source(AiAgentSource.FALLBACK.name())
                .suggestions(responseFactory.defaultSuggestions())
                .build();
    }

    private AgentResponse routeMessage(AgentMessageCommand command, String message) {
        AiAgentAnalysis analysis = resolveAnalysis(message, command.clientAction());
        analysis = applyConversationContext(command.sessionId(), message, analysis);
        log.debug("AI agent route intent={}, source={}, confidence={}", analysis.intent(), analysis.source(), analysis.confidence());

        if (validator.lowConfidence(analysis, MIN_CONFIDENCE)) {
            if (looksAmbiguousForImportantWrite(message)) {
                return naturalGeneralReply(message, analysis);
            }
            return responseFactory.base(
                            "M\u00ecnh ch\u01b0a ch\u1eafc b\u1ea1n mu\u1ed1n l\u00e0m g\u00ec. B\u1ea1n c\u00f3 th\u1ec3 n\u00f3i r\u00f5 h\u01a1n, v\u00ed d\u1ee5: \"t\u00ecm s\u00e1ch kinh t\u1ebf\", \"th\u00eam 2 cu\u1ed1n Clean Code v\u00e0o gi\u1ecf\", ho\u1eb7c \"\u0111\u01a1n h\u00e0ng g\u1ea7n nh\u1ea5t c\u1ee7a t\u00f4i\".",
                            analysis)
                    .suggestions(responseFactory.defaultSuggestions())
                    .build();
        }
        AiAgentIntent intent = analysis.intent().normalized();
        if (validator.requiresAuth(intent) && command.userId() == null) {
            return loginRequired(analysis);
        }

        return switch (intent) {
            case SEARCH_BOOK, RECOMMEND_BOOK -> searchBooks(message, analysis);
            case VIEW_BOOK_DETAIL -> bookDetail(analysis, message);
            case CHECK_STOCK -> checkStock(analysis, message);
            case VIEW_CART -> viewCart(command.userId(), analysis);
            case VIEW_ORDER, CHECK_ORDER_STATUS -> orderStatus(command.userId(), analysis);
            case VIEW_LATEST_ORDER -> latestOrder(command.userId(), analysis);
            case ADD_TO_CART -> executeLightWrite(command.userId(), message, analysis, this::executeAddToCart);
            case UPDATE_CART_QUANTITY -> executeLightWrite(command.userId(), message, analysis, this::executeUpdateCart);
            case REMOVE_FROM_CART -> executeLightWrite(command.userId(), message, analysis, this::executeRemoveCart);
            case PLACE_ORDER, CANCEL_ORDER, PAY_ORDER, CHANGE_SHIPPING_ADDRESS -> createPendingAction(command, message, analysis);
            case UNKNOWN -> naturalGeneralReply(message, analysis);
            default -> naturalGeneralReply(message, analysis);
        };
    }

    private AiAgentAnalysis resolveAnalysis(String message, ClientAction clientAction) {
        AiAgentAnalysis rule = ruleEngine.analyze(message, clientAction).withDefaults(AiAgentSource.RULE);
        if (rule.confidence() >= RULE_THRESHOLD && rule.intent().normalized() != AiAgentIntent.UNKNOWN) {
            return rule;
        }
        AiAgentAnalysis gemini = geminiIntentParser.parse(message).withDefaults(AiAgentSource.GEMINI);
        if (looksAmbiguousForImportantWrite(message) && gemini.intent().normalized().isImportantWrite()) {
            return AiAgentAnalysis.unknown(AiAgentSource.GEMINI, "ambiguous important write");
        }
        return gemini.intent().normalized() == AiAgentIntent.UNKNOWN && rule.confidence() > gemini.confidence()
                ? rule
                : gemini;
    }

    private AiAgentAnalysis applyConversationContext(String sessionId, String message, AiAgentAnalysis analysis) {
        if (sessionId == null || sessionId.isBlank() || analysis == null || analysis.entities() == null || analysis.intent() == null) {
            return analysis;
        }
        AiAgentIntent intent = analysis.intent().normalized();
        if (!(intent == AiAgentIntent.PLACE_ORDER
                || intent == AiAgentIntent.ADD_TO_CART
                || intent == AiAgentIntent.UPDATE_CART_QUANTITY
                || intent == AiAgentIntent.REMOVE_FROM_CART
                || intent == AiAgentIntent.VIEW_BOOK_DETAIL
                || intent == AiAgentIntent.CHECK_STOCK)) {
            return analysis;
        }
        AiAgentEntities entities = analysis.entities();
        if (entities.bookId() != null || normalizeBlank(entities.bookName()) != null) {
            return analysis;
        }
        List<BookReference> context = sessionBookContext.getOrDefault(sessionId, List.of());
        Optional<BookReference> selected = resolveContextBook(message, context);
        if (selected.isEmpty()) {
            return analysis;
        }
        BookReference book = selected.get();
        AiAgentEntities enriched = new AiAgentEntities(
                book.title(),
                book.id(),
                entities.quantity(),
                entities.couponCode(),
                entities.paymentMethod(),
                entities.shippingAddress(),
                entities.customerPhone(),
                entities.orderId(),
                entities.category(),
                entities.author()
        );
        return new AiAgentAnalysis(
                intent,
                enriched,
                Math.max(analysis.confidence(), 0.9),
                analysis.source(),
                analysis.needConfirmation(),
                "conversation context: " + analysis.reason(),
                analysis.missingFields()
        );
    }

    private Optional<BookReference> resolveContextBook(String message, List<BookReference> context) {
        if (context == null || context.isEmpty()) {
            return Optional.empty();
        }
        String normalized = normalizeForSearch(message);
        if (normalized.contains("dau tien") || normalized.contains("thu nhat")) {
            return Optional.of(context.get(0));
        }
        boolean refersToPreviousBook = normalized.contains("cuon do")
                || normalized.contains("quyen do")
                || normalized.contains("sach do")
                || normalized.contains("cuon nay")
                || normalized.contains("quyen nay")
                || normalized.contains("sach nay");
        boolean genericTakeReference = normalized.contains("lay")
                && (normalized.contains("cuon") || normalized.contains("quyen") || normalized.contains("sach"));
        boolean suppliesCheckoutInfo = normalized.contains("dat hang")
                && (normalized.contains("cod")
                || normalized.contains("vnpay")
                || normalized.contains("dia chi")
                || normalized.contains("so dien thoai"));
        if ((refersToPreviousBook || genericTakeReference || suppliesCheckoutInfo) && context.size() == 1) {
            return Optional.of(context.get(0));
        }
        return Optional.empty();
    }

    private void rememberBookContext(String sessionId, AgentResponse response) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        if (response == null || response.books() == null || response.books().isEmpty()) {
            return;
        }
        List<BookReference> references = response.books().stream()
                .filter(book -> book.bookId() != null)
                .map(book -> new BookReference(book.bookId(), book.title()))
                .toList();
        if (!references.isEmpty()) {
            sessionBookContext.put(sessionId, references);
        }
    }

    private AgentResponse searchBooks(String message, AiAgentAnalysis analysis) {
        String query = catalogQuery(message, analysis);
        boolean allowVectorFallback = analysis != null && analysis.intent() != null
                && analysis.intent().normalized() == AiAgentIntent.RECOMMEND_BOOK;
        List<BookContext> books = findRelevantBooks(query, allowVectorFallback);
        if (books.isEmpty()) {
            return responseFactory.base("M\u00ecnh ch\u01b0a t\u00ecm th\u1ea5y s\u00e1ch ph\u00f9 h\u1ee3p trong kho SEBook. B\u1ea1n th\u1eed m\u00f4 t\u1ea3 th\u1ec3 lo\u1ea1i, t\u00e1c gi\u1ea3 ho\u1eb7c ch\u1ee7 \u0111\u1ec1 c\u1ee5 th\u1ec3 h\u01a1n nh\u00e9.", analysis)
                    .suggestions(responseFactory.defaultSuggestions())
                    .build();
        }
        List<BookResult> results = books.stream().map(this::toBookResult).toList();
        return responseFactory.base("M\u00ecnh t\u00ecm th\u1ea5y " + books.size() + " quy\u1ec3n s\u00e1ch ph\u00f9 h\u1ee3p.", analysis)
                .books(results)
                .cards(books.stream().map(responseFactory::bookCard).toList())
                .suggestions(List.of("Th\u00eam s\u00e1ch v\u00e0o gi\u1ecf", "Xem chi ti\u1ebft s\u00e1ch", "T\u00ecm s\u00e1ch kh\u00e1c"))
                .build();
    }

    private String catalogQuery(String message, AiAgentAnalysis analysis) {
        if (analysis != null && analysis.entities() != null) {
            AiAgentEntities entities = analysis.entities();
            if (normalizeBlank(entities.bookName()) != null) {
                return entities.bookName();
            }
            if (normalizeBlank(entities.author()) != null) {
                return entities.author();
            }
            if (normalizeBlank(entities.category()) != null) {
                return entities.category();
            }
        }
        return message;
    }

    private AgentResponse bookDetail(AiAgentAnalysis analysis, String originalMessage) {
        BookContext book = resolveBook(analysis.entities(), originalMessage);
        if (book == null) {
            return searchBooks(originalMessage, analysis);
        }
        return responseFactory.base("\u0110\u00e2y l\u00e0 th\u00f4ng tin s\u00e1ch \"" + book.title() + "\".", analysis)
                .books(List.of(toBookResult(book)))
                .cards(List.of(responseFactory.bookCard(book)))
                .suggestions(List.of("Th\u00eam v\u00e0o gi\u1ecf", "T\u00ecm s\u00e1ch t\u01b0\u01a1ng t\u1ef1", "Xem gi\u1ecf h\u00e0ng"))
                .build();
    }

    private AgentResponse checkStock(AiAgentAnalysis analysis, String originalMessage) {
        BookContext book = resolveBook(analysis.entities(), originalMessage);
        if (book == null) {
            return responseFactory.base("M\u00ecnh ch\u01b0a t\u00ecm th\u1ea5y s\u00e1ch b\u1ea1n mu\u1ed1n ki\u1ec3m tra t\u1ed3n kho. B\u1ea1n nh\u1eadp r\u00f5 h\u01a1n t\u00ean s\u00e1ch gi\u00fap m\u00ecnh nh\u00e9.", analysis)
                    .suggestions(List.of("T\u00ecm s\u00e1ch kh\u00e1c", "Xem s\u00e1ch b\u00e1n ch\u1ea1y", "Xem gi\u1ecf h\u00e0ng"))
                    .build();
        }
        String stockText = book.quantity() > 0
                ? "S\u00e1ch \"" + book.title() + "\" hi\u1ec7n c\u00f2n " + book.quantity() + " cu\u1ed1n trong kho."
                : "S\u00e1ch \"" + book.title() + "\" hi\u1ec7n \u0111ang h\u1ebft h\u00e0ng.";
        return responseFactory.base(stockText, analysis)
                .books(List.of(toBookResult(book)))
                .cards(List.of(responseFactory.bookCard(book)))
                .suggestions(book.quantity() > 0
                        ? List.of("Th\u00eam v\u00e0o gi\u1ecf", "Xem chi ti\u1ebft s\u00e1ch", "T\u00ecm s\u00e1ch t\u01b0\u01a1ng t\u1ef1")
                        : List.of("T\u00ecm s\u00e1ch t\u01b0\u01a1ng t\u1ef1", "Xem s\u00e1ch kh\u00e1c", "Xem chi ti\u1ebft s\u00e1ch"))
                .build();
    }

    private AgentResponse viewCart(Long userId, AiAgentAnalysis analysis) {
        CartResult cart = toCartResult(cartUseCase.getCartByUserId(userId));
        return responseFactory.base("\u0110\u00e2y l\u00e0 gi\u1ecf h\u00e0ng hi\u1ec7n t\u1ea1i c\u1ee7a b\u1ea1n.", analysis)
                .cart(cart)
                .cards(List.of(AgentCard.builder()
                        .type("CART_CARD")
                        .title("Gi\u1ecf h\u00e0ng")
                        .price(cart.totalAmount())
                        .url("/cart")
                        .actions(List.of(AgentAction.builder().label("Xem gi\u1ecf h\u00e0ng").action("VIEW_CART").url("/cart").build()))
                        .build()))
                .suggestions(List.of("\u0110\u1eb7t h\u00e0ng COD", "\u0110\u1eb7t h\u00e0ng VNPAY", "T\u00ecm th\u00eam s\u00e1ch"))
                .build();
    }

    private AgentResponse latestOrder(Long userId, AiAgentAnalysis analysis) {
        List<OrderInternalUseCase.OrderResponse> orders = orderUseCase.getMyOrders(userId);
        if (orders.isEmpty()) {
            return responseFactory.base("B\u1ea1n ch\u01b0a c\u00f3 \u0111\u01a1n h\u00e0ng n\u00e0o.", analysis)
                    .suggestions(responseFactory.defaultSuggestions())
                    .build();
        }
        OrderInternalUseCase.OrderResponse newest = orders.stream()
                .max(Comparator.comparing(OrderInternalUseCase.OrderResponse::getUpdatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(orders.get(0));
        OrderResult order = toOrderResult(newest, null);
        return responseFactory.base("\u0110\u01a1n g\u1ea7n nh\u1ea5t c\u1ee7a b\u1ea1n l\u00e0 #" + newest.getOrderId() + ", tr\u1ea1ng th\u00e1i " + fulfillmentStatusLabel(newest.getFulfillmentStatus()) + ".", analysis)
                .order(order)
                .cards(List.of(responseFactory.orderCard(order, null)))
                .suggestions(List.of("Xem gi\u1ecf h\u00e0ng", "T\u00ecm s\u00e1ch m\u1edbi"))
                .build();
    }

    private AgentResponse orderStatus(Long userId, AiAgentAnalysis analysis) {
        Long orderId = analysis.entities().orderId();
        if (orderId == null) {
            return latestOrder(userId, analysis);
        }
        OrderInternalUseCase.OrderResponse response = orderUseCase.getMyOrderById(orderId, userId);
        OrderResult order = toOrderResult(response, null);
        return responseFactory.base("\u0110\u01a1n #" + response.getOrderId() + " hi\u1ec7n \u0111ang \u1edf tr\u1ea1ng th\u00e1i " + fulfillmentStatusLabel(response.getFulfillmentStatus()) + ".", analysis)
                .order(order)
                .cards(List.of(responseFactory.orderCard(order, null)))
                .suggestions(responseFactory.defaultSuggestions())
                .build();
    }

    private AgentResponse executeLightWrite(Long userId, String message, AiAgentAnalysis analysis, LightWriteExecutor executor) {
        Optional<AgentResponse> ambiguousSelection = ambiguousBookSelectionResponse(message, analysis);
        if (ambiguousSelection.isPresent()) {
            return ambiguousSelection.get();
        }
        ActionPayload payload = buildPayload(message, analysis);
        List<String> missing = validator.missingFields(analysis.intent(), payload);
        if (!missing.isEmpty()) {
            return responseFactory.base("M\u00ecnh c\u1ea7n bi\u1ebft r\u00f5 " + String.join(", ", missing) + " tr\u01b0\u1edbc khi thao t\u00e1c.", analysis)
                    .suggestions(List.of("T\u00ecm s\u00e1ch", "Xem gi\u1ecf h\u00e0ng", "T\u00ecm s\u00e1ch b\u00e1n ch\u1ea1y"))
                    .build();
        }
        if (payload.bookId() != null) {
            Optional<AgentResponse> invalid = inventoryValidationResponse(payload, analysis);
            if (invalid.isPresent()) {
                return invalid.get();
            }
        }
        return executor.execute(userId, payload);
    }

    private AgentResponse createPendingAction(AgentMessageCommand command, String message, AiAgentAnalysis analysis) {
        Optional<AgentResponse> ambiguousSelection = ambiguousBookSelectionResponse(message, analysis);
        if (ambiguousSelection.isPresent()) {
            return ambiguousSelection.get();
        }
        ActionPayload payload = resolveOrderReference(command.userId(), message, analysis, buildPayload(message, analysis));
        if (payload.bookId() != null) {
            Optional<AgentResponse> invalid = inventoryValidationResponse(payload, analysis);
            if (invalid.isPresent()) {
                return invalid.get();
            }
        }
        List<String> missing = validator.missingFields(analysis.intent(), payload);
        if (!missing.isEmpty()) {
            BookContext book = payload.bookId() == null ? null : safeGetBook(payload.bookId());
            String prefix = book == null
                    ? "M\u00ecnh c\u1ea7n th\u00eam th\u00f4ng tin tr\u01b0\u1edbc khi th\u1ef1c hi\u1ec7n: "
                    : "M\u00ecnh \u0111\u00e3 t\u00ecm th\u1ea5y s\u00e1ch \"" + book.title() + "\". \u0110\u1ec3 \u0111\u1eb7t h\u00e0ng, m\u00ecnh c\u1ea7n th\u00eam: ";
            return responseFactory.base(prefix + String.join(", ", missing) + ".", analysis)
                    .books(book == null ? null : List.of(toBookResult(book)))
                    .cards(book == null ? null : List.of(responseFactory.bookCard(book)))
                    .suggestions(missingFieldSuggestions(missing))
                    .build();
        }
        AiAgentPendingAction action = AiAgentPendingAction.create(
                command.sessionId(),
                command.userId(),
                analysis.intent(),
                writePayload(payload),
                LocalDateTime.now().plusMinutes(PENDING_EXPIRY_MINUTES)
        );
        pendingActionPort.save(action);
        BigDecimal estimatedTotal = payload.unitPrice() == null ? null : payload.unitPrice().multiply(BigDecimal.valueOf(payload.quantity()));
        return responseFactory.base("B\u1ea1n x\u00e1c nh\u1eadn th\u1ef1c hi\u1ec7n thao t\u00e1c n\u00e0y ch\u1ee9?", analysis)
                .confirmationCard(responseFactory.confirmationCard(action, payload))
                .pendingAction(responseFactory.pending(action, payload, estimatedTotal))
                .actions(responseFactory.pendingActions(action))
                .suggestions(List.of("X\u00e1c nh\u1eadn", "H\u1ee7y", "Xem gi\u1ecf h\u00e0ng"))
                .build();
    }

    private Optional<AgentResponse> inventoryValidationResponse(ActionPayload payload, AiAgentAnalysis analysis) {
        if (payload.quantity() <= 0) {
            return Optional.of(responseFactory.base("S\u1ed1 l\u01b0\u1ee3ng kh\u00f4ng h\u1ee3p l\u1ec7. B\u1ea1n vui l\u00f2ng nh\u1eadp s\u1ed1 l\u01b0\u1ee3ng l\u1edbn h\u01a1n 0.", analysis)
                    .error(responseFactory.error("INVALID_QUANTITY", "S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i l\u1edbn h\u01a1n 0"))
                    .suggestions(List.of("Nh\u1eadp l\u1ea1i s\u1ed1 l\u01b0\u1ee3ng", "T\u00ecm s\u00e1ch", "Xem gi\u1ecf h\u00e0ng"))
                    .build());
        }
        BookContext book = safeGetBook(payload.bookId());
        if (book == null || !book.isActive()) {
            return Optional.of(responseFactory.base("M\u00ecnh kh\u00f4ng t\u00ecm th\u1ea5y s\u00e1ch c\u1ea7n thao t\u00e1c.", analysis)
                    .error(responseFactory.error("BOOK_NOT_FOUND", "Kh\u00f4ng t\u00ecm th\u1ea5y s\u00e1ch"))
                    .suggestions(responseFactory.defaultSuggestions())
                    .build());
        }
        if (book.quantity() < payload.quantity()) {
            return Optional.of(responseFactory.base("Kh\u00f4ng \u0111\u1ee7 h\u00e0ng. S\u00e1ch \"" + book.title() + "\" hi\u1ec7n ch\u1ec9 c\u00f2n " + book.quantity() + " cu\u1ed1n.", analysis)
                    .books(List.of(toBookResult(book)))
                    .cards(List.of(responseFactory.bookCard(book)))
                    .error(responseFactory.error("INSUFFICIENT_STOCK", "Kh\u00f4ng \u0111\u1ee7 h\u00e0ng"))
                    .suggestions(List.of("Ch\u1ecdn s\u1ed1 l\u01b0\u1ee3ng kh\u00e1c", "Xem chi ti\u1ebft s\u00e1ch", "T\u00ecm s\u00e1ch kh\u00e1c"))
                    .build());
        }
        return Optional.empty();
    }

    private ActionPayload resolveOrderReference(Long userId, String message, AiAgentAnalysis analysis, ActionPayload payload) {
        if (analysis == null || analysis.intent() == null || payload == null || payload.orderId() != null) {
            return payload;
        }
        AiAgentIntent intent = analysis.intent().normalized();
        if (!(intent == AiAgentIntent.CANCEL_ORDER || intent == AiAgentIntent.PAY_ORDER)) {
            return payload;
        }
        if (!looksLikeLatestOrderReference(message)) {
            return payload;
        }
        List<OrderInternalUseCase.OrderResponse> orders = orderUseCase.getMyOrders(userId);
        if (orders == null || orders.isEmpty()) {
            return payload;
        }
        Long latestOrderId = orders.stream()
                .max(Comparator.comparing(OrderInternalUseCase.OrderResponse::getUpdatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(OrderInternalUseCase.OrderResponse::getOrderId)
                .orElse(null);
        return latestOrderId == null ? payload : withOrderId(payload, latestOrderId);
    }

    private ActionPayload withOrderId(ActionPayload payload, Long orderId) {
        return new ActionPayload(
                payload.bookId(),
                payload.bookTitle(),
                payload.quantity(),
                payload.couponCode(),
                payload.paymentMethod(),
                payload.shippingAddress(),
                payload.customerPhone(),
                payload.selectedBookIds(),
                orderId,
                payload.unitPrice(),
                payload.cartCheckout()
        );
    }

    private boolean looksLikeLatestOrderReference(String message) {
        String normalized = normalizeForSearch(message);
        return normalized.contains("gan nhat")
                || normalized.contains("moi dat")
                || normalized.contains("hoi nay")
                || normalized.contains("vua dat")
                || normalized.contains("don moi");
    }

    private AgentResponse executeAddToCart(Long userId, ActionPayload payload) {
        validateBookStock(payload);
        cartUseCase.addItem(userId, CartInternalUseCase.AddItemCommand.builder()
                .bookId(payload.bookId())
                .quantity(payload.quantity())
                .build());
        String bookName = payload.bookTitle() == null ? "s\u00e1ch" : "\"" + payload.bookTitle() + "\"";
        return AgentResponse.builder()
                .message("\u0110\u00e3 th\u00eam " + payload.quantity() + " cu\u1ed1n " + bookName + " v\u00e0o gi\u1ecf h\u00e0ng.")
                .intent(AiAgentIntent.ADD_TO_CART)
                .source(AiAgentSource.FALLBACK.name())
                .confidence(1.0)
                .cart(toCartResult(cartUseCase.getCartByUserId(userId)))
                .suggestions(List.of("Xem gi\u1ecf h\u00e0ng", "\u0110\u1eb7t h\u00e0ng COD", "T\u00ecm th\u00eam s\u00e1ch"))
                .build();
    }

    private AgentResponse executeUpdateCart(Long userId, ActionPayload payload) {
        validateBookStock(payload);
        cartUseCase.updateItemQuantity(userId, CartInternalUseCase.UpdateQuantityCommand.builder()
                .bookId(payload.bookId())
                .quantity(payload.quantity())
                .build());
        return AgentResponse.builder()
                .message("\u0110\u00e3 c\u1eadp nh\u1eadt s\u1ed1 l\u01b0\u1ee3ng trong gi\u1ecf h\u00e0ng.")
                .intent(AiAgentIntent.UPDATE_CART_QUANTITY)
                .source(AiAgentSource.FALLBACK.name())
                .confidence(1.0)
                .cart(toCartResult(cartUseCase.getCartByUserId(userId)))
                .suggestions(List.of("Xem gi\u1ecf h\u00e0ng", "\u0110\u1eb7t h\u00e0ng", "T\u00ecm th\u00eam s\u00e1ch"))
                .build();
    }

    private AgentResponse executeRemoveCart(Long userId, ActionPayload payload) {
        cartUseCase.removeItem(userId, payload.bookId());
        return AgentResponse.builder()
                .message("\u0110\u00e3 x\u00f3a s\u00e1ch kh\u1ecfi gi\u1ecf h\u00e0ng.")
                .intent(AiAgentIntent.REMOVE_FROM_CART)
                .source(AiAgentSource.FALLBACK.name())
                .confidence(1.0)
                .cart(toCartResult(cartUseCase.getCartByUserId(userId)))
                .suggestions(responseFactory.defaultSuggestions())
                .build();
    }

    private AgentResponse executePlaceOrder(Long userId, ActionPayload payload, String ipAddress) {
        BookContext current = payload.bookId() == null ? null : validateBookStock(payload);
        if (current != null && payload.unitPrice() != null && current.price() != null && payload.unitPrice().compareTo(current.price()) != 0) {
            return AgentResponse.builder()
                    .message("Gi\u00e1 s\u00e1ch \"" + current.title() + "\" v\u1eeba thay \u0111\u1ed5i. B\u1ea1n vui l\u00f2ng t\u1ea1o l\u1ea1i y\u00eau c\u1ea7u \u0111\u1ec3 x\u00e1c nh\u1eadn theo gi\u00e1 m\u1edbi.")
                    .intent(AiAgentIntent.PLACE_ORDER)
                    .source(AiAgentSource.FALLBACK.name())
                    .error(responseFactory.error("PRICE_CHANGED", "Gi\u00e1 s\u00e1ch \u0111\u00e3 thay \u0111\u1ed5i"))
                    .books(List.of(toBookResult(current)))
                    .cards(List.of(responseFactory.bookCard(current)))
                    .build();
        }
        if (payload.bookId() != null) {
            ensureCartHasRequestedQuantity(userId, payload);
        }
        OrderInternalUseCase.CheckoutCommand command = OrderInternalUseCase.CheckoutCommand.builder()
                .requestId("ai-agent-" + UUID.randomUUID())
                .shippingAddress(payload.shippingAddress())
                .customerPhone(payload.customerPhone())
                .couponCode(payload.couponCode())
                .paymentMethod(payload.paymentMethod())
                .selectedBookIds(payload.selectedBookIds())
                .selectedBookQuantities(payload.bookId() == null ? null : Map.of(payload.bookId(), payload.quantity()))
                .build();
        OrderInternalUseCase.OrderResponse order = orderUseCase.checkout(userId, command);
        String redirectUrl = null;
        if ("VNPAY".equalsIgnoreCase(payload.paymentMethod())) {
            redirectUrl = paymentUseCase.createPaymentUrl(order.getOrderId(), userId, ipAddress);
        }
        OrderResult orderResult = toOrderResult(order, payload.paymentMethod());
        CartResult refreshedCart = toCartResult(cartUseCase.getCartByUserId(userId));
        return AgentResponse.builder()
                .message(redirectUrl == null
                        ? "\u0110\u00e3 t\u1ea1o \u0111\u01a1n h\u00e0ng #" + order.getOrderId() + "."
                        : "\u0110\u00e3 t\u1ea1o \u0111\u01a1n h\u00e0ng #" + order.getOrderId() + ". B\u1ea1n b\u1ea5m n\u00fat thanh to\u00e1n VNPAY \u0111\u1ec3 ti\u1ebfp t\u1ee5c.")
                .intent(AiAgentIntent.PLACE_ORDER)
                .source(AiAgentSource.FALLBACK.name())
                .confidence(1.0)
                .order(orderResult)
                .cart(refreshedCart)
                .cards(List.of(responseFactory.orderCard(orderResult, redirectUrl)))
                .redirectUrl(redirectUrl)
                .suggestions(responseFactory.defaultSuggestions())
                .build();
    }

    private void ensureCartHasRequestedQuantity(Long userId, ActionPayload payload) {
        CartInternalUseCase.CartResponse cart = cartUseCase.getCartByUserId(userId);
        int currentQuantity = cart == null || cart.getItems() == null ? 0 : cart.getItems().stream()
                .filter(item -> item.getBookId().equals(payload.bookId()))
                .map(CartInternalUseCase.CartItemResponse::getQuantity)
                .findFirst()
                .orElse(0);
        int missingQuantity = payload.quantity() - currentQuantity;
        if (missingQuantity > 0) {
            cartUseCase.addItem(userId, CartInternalUseCase.AddItemCommand.builder()
                    .bookId(payload.bookId())
                    .quantity(missingQuantity)
                    .build());
        }
    }

    private AgentResponse executeCancelOrder(Long userId, ActionPayload payload) {
        OrderInternalUseCase.OrderResponse order = orderUseCase.cancelMyPendingOrder(payload.orderId(), userId, "AI agent cancel request");
        OrderResult result = toOrderResult(order, null);
        return AgentResponse.builder()
                .message("\u0110\u00e3 h\u1ee7y \u0111\u01a1n h\u00e0ng #" + order.getOrderId() + ".")
                .intent(AiAgentIntent.CANCEL_ORDER)
                .source(AiAgentSource.FALLBACK.name())
                .confidence(1.0)
                .order(result)
                .cards(List.of(responseFactory.orderCard(result, null)))
                .suggestions(responseFactory.defaultSuggestions())
                .build();
    }

    private AgentResponse executePayOrder(Long userId, ActionPayload payload, String ipAddress) {
        OrderInternalUseCase.OrderResponse order = orderUseCase.getMyOrderById(payload.orderId(), userId);
        String redirectUrl = paymentUseCase.createPaymentUrl(order.getOrderId(), userId, ipAddress);
        OrderResult result = toOrderResult(order, "VNPAY");
        return AgentResponse.builder()
                .message("M\u00ecnh \u0111\u00e3 t\u1ea1o link thanh to\u00e1n VNPAY cho \u0111\u01a1n #" + order.getOrderId() + ".")
                .intent(AiAgentIntent.PAY_ORDER)
                .source(AiAgentSource.FALLBACK.name())
                .confidence(1.0)
                .order(result)
                .cards(List.of(responseFactory.orderCard(result, redirectUrl)))
                .redirectUrl(redirectUrl)
                .suggestions(responseFactory.defaultSuggestions())
                .build();
    }
    private AgentResponse unsupportedImportantAction(AiAgentIntent intent) {
        return AgentResponse.builder()
                .message("Thao t\u00e1c n\u00e0y ch\u01b0a \u0111\u01b0\u1ee3c h\u1ed7 tr\u1ee3 qua AI Agent.")
                .intent(intent.normalized())
                .source(AiAgentSource.FALLBACK.name())
                .error(responseFactory.error("UNSUPPORTED_ACTION", "Ch\u01b0a h\u1ed7 tr\u1ee3 thao t\u00e1c"))
                .suggestions(responseFactory.defaultSuggestions())
                .build();
    }

    private AgentResponse naturalGeneralReply(String message, AiAgentAnalysis analysis) {
        String answer = naturalize("Tr\u1ea3 l\u1eddi th\u00e2n thi\u1ec7n cho kh\u00e1ch h\u00e0ng SEBook. N\u1ebfu ph\u00f9 h\u1ee3p, h\u01b0\u1edbng d\u1eabn kh\u00e1ch h\u1ecfi v\u1ec1 s\u00e1ch, gi\u1ecf h\u00e0ng ho\u1eb7c \u0111\u01a1n h\u00e0ng. C\u00e2u h\u1ecfi: " + message);
        return responseFactory.base(answer, analysis)
                .suggestions(responseFactory.defaultSuggestions())
                .build();
    }

    private String naturalize(String prompt) {
        try {
            return llmPort.chat(prompt, List.of());
        } catch (Exception e) {
            log.warn("Unable to generate natural AI agent response: {}", e.getMessage());
            return "M\u00ecnh c\u00f3 th\u1ec3 gi\u00fap b\u1ea1n t\u00ecm s\u00e1ch, ki\u1ec3m tra t\u1ed3n kho, th\u00eam v\u00e0o gi\u1ecf h\u00e0ng ho\u1eb7c xem \u0111\u01a1n h\u00e0ng. B\u1ea1n mu\u1ed1n l\u00e0m g\u00ec ti\u1ebfp theo?";
        }
    }

    private ActionPayload buildPayload(String message, AiAgentAnalysis analysis) {
        AiAgentEntities entities = analysis.entities() == null ? new AiAgentEntities() : analysis.entities();
        BookContext book = resolveBookFromEntities(entities);
        Long bookId = book != null ? book.id() : entities.bookId();
        List<Long> selectedBookIds = bookId == null ? null : List.of(bookId);
        boolean cartCheckout = isCartCheckoutRequest(message, analysis, bookId, entities);
        return new ActionPayload(
                bookId,
                book != null ? book.title() : normalizeBlank(entities.bookName()),
                entities.normalizedQuantity(),
                normalizeBlank(entities.couponCode()),
                normalizePaymentMethod(entities.paymentMethod()),
                normalizeBlank(entities.shippingAddress()),
                normalizeBlank(entities.customerPhone()),
                selectedBookIds,
                entities.orderId(),
                book == null ? null : book.price(),
                cartCheckout
        );
    }

    private Optional<AgentResponse> ambiguousBookSelectionResponse(String message, AiAgentAnalysis analysis) {
        if (analysis == null || analysis.entities() == null || analysis.intent() == null) {
            return Optional.empty();
        }
        AiAgentIntent intent = analysis.intent().normalized();
        if (!(intent.isImportantWrite() || intent.isLightWrite())) {
            return Optional.empty();
        }
        AiAgentEntities entities = analysis.entities();
        if (entities.bookId() != null || normalizeBlank(entities.bookName()) == null) {
            return Optional.empty();
        }
        List<BookContext> candidates = findRelevantBooks(entities.bookName(), false);
        if (candidates.size() <= 1 || exactBookMatch(candidates, entities.bookName()).isPresent()) {
            return Optional.empty();
        }
        List<BookResult> results = candidates.stream().map(this::toBookResult).toList();
        return Optional.of(responseFactory.base("M\u00ecnh t\u00ecm th\u1ea5y nhi\u1ec1u s\u00e1ch ph\u00f9 h\u1ee3p. B\u1ea1n ch\u1ecdn \u0111\u00fang quy\u1ec3n mu\u1ed1n thao t\u00e1c nh\u00e9.", analysis)
                .books(results)
                .cards(candidates.stream().map(responseFactory::bookCard).toList())
                .suggestions(List.of("Ch\u1ecdn s\u00e1ch", "Xem chi ti\u1ebft s\u00e1ch", "T\u00ecm s\u00e1ch kh\u00e1c"))
                .build());
    }

    private boolean isCartCheckoutRequest(String message, AiAgentAnalysis analysis, Long bookId, AiAgentEntities entities) {
        if (analysis == null || analysis.intent() == null || analysis.intent().normalized() != AiAgentIntent.PLACE_ORDER) {
            return false;
        }
        if (bookId != null || normalizeBlank(entities.bookName()) != null) {
            return false;
        }
        String paymentMethod = normalizePaymentMethod(entities.paymentMethod());
        String normalized = normalizeForSearch(message);
        return paymentMethod != null
                || normalized.contains("dat hang cod")
                || normalized.contains("dat hang vnpay")
                || normalized.contains("checkout")
                || normalized.contains("thanh toan");
    }

    private BookContext resolveBookFromEntities(AiAgentEntities entities) {
        if (entities.bookId() != null) {
            return safeGetBook(entities.bookId());
        }
        String title = normalizeBlank(entities.bookName());
        if (title == null) {
            return null;
        }
        List<BookContext> candidates = findRelevantBooks(title);
        return exactBookMatch(candidates, title).orElseGet(() -> candidates.size() == 1 ? candidates.get(0) : null);
    }

    private BookContext validateBookStock(ActionPayload payload) {
        if (payload.bookId() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Kh\u00f4ng t\u00ecm th\u1ea5y s\u00e1ch c\u1ea7n thao t\u00e1c");
        }
        if (payload.quantity() <= 0) {
            throw new AppException(ErrorCode.INVALID_INPUT, "S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i l\u1edbn h\u01a1n 0");
        }
        BookContext book = safeGetBook(payload.bookId());
        if (book == null || !book.isActive()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Kh\u00f4ng t\u00ecm th\u1ea5y s\u00e1ch");
        }
        if (book.quantity() < payload.quantity()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "S\u00e1ch \"" + book.title() + "\" hi\u1ec7n ch\u1ec9 c\u00f2n " + book.quantity() + " cu\u1ed1n");
        }
        return book;
    }

    private List<BookContext> findRelevantBooks(String userMessage) {
        return findRelevantBooks(userMessage, true);
    }

    private List<BookContext> findRelevantBooks(String userMessage, boolean allowVectorFallback) {
        try {
            List<BookContext> catalogBooks = safeSearchBooks(userMessage).stream()
                    .map(document -> safeGetBook(document.id()))
                    .filter(Objects::nonNull)
                    .filter(BookContext::isActive)
                    .filter(book -> isRelevantToQuery(userMessage, book))
                    .limit(RAG_TOP_K)
                    .toList();
            if (!catalogBooks.isEmpty()) {
                return catalogBooks;
            }
            List<BookContext> broadCatalogBooks = safeSearchBooks(null).stream()
                    .map(document -> safeGetBook(document.id()))
                    .filter(Objects::nonNull)
                    .filter(BookContext::isActive)
                    .filter(book -> isRelevantToQuery(userMessage, book))
                    .limit(RAG_TOP_K)
                    .toList();
            if (!broadCatalogBooks.isEmpty()) {
                return broadCatalogBooks;
            }
            if (!allowVectorFallback) {
                return List.of();
            }
            List<Long> bookIds = vectorStorePort.findSimilarBooks(userMessage, RAG_TOP_K);
            if (bookIds == null || bookIds.isEmpty()) return List.of();
            return bookIds.stream()
                    .map(this::safeGetBook)
                    .filter(Objects::nonNull)
                    .filter(BookContext::isActive)
                    .filter(book -> isRelevantToQuery(userMessage, book))
                    .toList();
        } catch (Exception e) {
            log.warn("Unable to retrieve agent catalog context: {}", e.getMessage());
            return List.of();
        }
    }

    private List<CatalogBookPort.BookDocument> safeSearchBooks(String query) {
        List<CatalogBookPort.BookDocument> books = catalogBookPort.searchBooks(query, null);
        return books == null ? List.of() : books;
    }

    private BookContext resolveBook(AiAgentEntities entities, String message) {
        if (entities.bookId() != null) {
            return safeGetBook(entities.bookId());
        }
        String title = normalizeBlank(entities.bookName()) != null ? entities.bookName() : message;
        List<BookContext> candidates = findRelevantBooks(title);
        return exactBookMatch(candidates, title).orElseGet(() -> candidates.size() == 1 ? candidates.get(0) : null);
    }

    private Optional<BookContext> exactBookMatch(List<BookContext> books, String query) {
        String normalizedQuery = normalizeForSearch(query);
        if (normalizedQuery.isBlank()) {
            return Optional.empty();
        }
        return books.stream()
                .filter(book -> normalizeForSearch(book.title()).equals(normalizedQuery))
                .findFirst();
    }

    private boolean isRelevantToQuery(String query, BookContext book) {
        List<String> tokens = queryTokens(query);
        if (tokens.isEmpty()) {
            return true;
        }
        String haystack = normalizeForSearch(String.join(" ",
                nullToBlank(book.title()),
                nullToBlank(book.author()),
                nullToBlank(book.description()),
                book.keywords() == null ? "" : String.join(" ", book.keywords())
        ));
        if (tokens.size() == 1) {
            return haystack.contains(tokens.get(0));
        }
        return tokens.stream().allMatch(haystack::contains);
    }

    private List<String> queryTokens(String query) {
        String normalized = normalizeForSearch(query);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split("\\s+"))
                .filter(token -> token.length() >= 3)
                .filter(token -> !Set.of("sach", "quyen", "cuon", "tim", "giup", "khong", "cua", "ve", "giong", "tuong").contains(token))
                .distinct()
                .toList();
    }

    private String normalizeForSearch(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('\u0111', 'd')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private BookContext safeGetBook(Long bookId) {
        try {
            return catalogBookPort.getBook(bookId);
        } catch (Exception e) {
            return null;
        }
    }

    private CartResult toCartResult(CartInternalUseCase.CartResponse cart) {
        if (cart == null) {
            return CartResult.builder()
                    .totalAmount(BigDecimal.ZERO)
                    .items(List.of())
                    .build();
        }
        return CartResult.builder()
                .totalAmount(cart.getTotalAmount())
                .items((cart.getItems() == null ? List.<CartInternalUseCase.CartItemResponse>of() : cart.getItems()).stream()
                        .map(item -> CartItemResult.builder()
                                .bookId(item.getBookId())
                                .title(item.getTitle())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();
    }

    private OrderResult toOrderResult(OrderInternalUseCase.OrderResponse order, String paymentMethod) {
        return OrderResult.builder()
                .orderId(order.getOrderId())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .fulfillmentStatus(order.getFulfillmentStatus())
                .paymentMethod(paymentMethod)
                .build();
    }

    private String fulfillmentStatusLabel(String status) {
        if (status == null) {
            return "Kh\u00f4ng r\u00f5";
        }
        return switch (status) {
            case "PENDING" -> "Ch\u1edd thanh to\u00e1n";
            case "CONFIRMED" -> "Ch\u1edd x\u00e1c nh\u1eadn";
            case "PROCESSING" -> "\u0110ang x\u1eed l\u00fd";
            case "DELIVERING" -> "\u0110ang giao";
            case "DELIVERED" -> "\u0110\u00e3 giao";
            case "CANCELLED" -> "\u0110\u00e3 h\u1ee7y";
            default -> status;
        };
    }

    private BookResult toBookResult(BookContext book) {
        return BookResult.builder()
                .bookId(book.id())
                .title(book.title())
                .author(book.author())
                .price(book.price())
                .quantity(book.quantity())
                .imageUrl(book.imageUrl())
                .description(book.description())
                .build();
    }

    private AgentResponse loginRequired(AiAgentAnalysis analysis) {
        return responseFactory.base("B\u1ea1n c\u1ea7n \u0111\u0103ng nh\u1eadp \u0111\u1ec3 m\u00ecnh c\u00f3 th\u1ec3 thao t\u00e1c gi\u1ecf h\u00e0ng ho\u1eb7c \u0111\u01a1n h\u00e0ng gi\u00fap b\u1ea1n.", analysis)
                .suggestions(List.of("\u0110\u0103ng nh\u1eadp", "T\u00ecm s\u00e1ch", "Xem s\u00e1ch b\u00e1n ch\u1ea1y"))
                .error(responseFactory.error("AUTH_REQUIRED", "C\u1ea7n \u0111\u0103ng nh\u1eadp"))
                .build();
    }

    private List<String> missingFieldSuggestions(List<String> missingFields) {
        List<String> suggestions = new ArrayList<>();
        String normalizedMissing = normalizeForSearch(String.join(" ", missingFields));
        if (normalizedMissing.contains("dia chi")) {
            suggestions.add("Ch\u1ecdn \u0111\u1ecba ch\u1ec9 giao h\u00e0ng");
        }
        if (normalizedMissing.contains("dien thoai") || normalizedMissing.contains("phone")) {
            suggestions.add("Nh\u1eadp s\u1ed1 \u0111i\u1ec7n tho\u1ea1i");
        }
        if (normalizedMissing.contains("phuong thuc") || normalizedMissing.contains("thanh toan")) {
            suggestions.add("Ch\u1ecdn COD");
            suggestions.add("Ch\u1ecdn VNPAY");
        }
        if (normalizedMissing.contains("sach")) {
            suggestions.add("T\u00ecm s\u00e1ch");
        }
        if (suggestions.isEmpty()) {
            suggestions.addAll(responseFactory.defaultSuggestions());
        }
        return suggestions;
    }

    private String effectiveMessage(AgentMessageCommand command) {
        if (command.message() != null && !command.message().isBlank()) {
            return command.message();
        }
        if (command.clientAction() != null && command.clientAction().message() != null) {
            return command.clientAction().message();
        }
        return "";
    }

    private void saveMessage(String sessionId, Long customerId, ChatRole role, String content) {
        if (sessionId == null || sessionId.isBlank() || content == null || content.isBlank()) {
            return;
        }
        ChatSession session = historyPort.findById(sessionId)
                .orElseGet(() -> ChatSession.create(sessionId, customerId));
        session.markActive();
        historyPort.saveSession(session);
        historyPort.saveMessage(ChatMessage.create(sessionId, role, content));
    }

    private String writePayload(ActionPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Kh\u00f4ng th\u1ec3 l\u01b0u thao t\u00e1c AI");
        }
    }

    private ActionPayload readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, ActionPayload.class);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_INPUT, "D\u1eef li\u1ec7u thao t\u00e1c AI kh\u00f4ng h\u1ee3p l\u1ec7");
        }
    }

    private String normalizePaymentMethod(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT);
        if (normalized.contains("COD") || normalized.contains("TIEN MAT") || normalized.contains("KHI NHAN")) {
            return "COD";
        }
        if (normalized.contains("VNPAY") || normalized.contains("VN PAY") || normalized.contains("ONLINE")) {
            return "VNPAY";
        }
        return null;
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean looksAmbiguousForImportantWrite(String value) {
        String normalized = value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('\u0111', 'd');
        return normalized.contains("co nen mua")
                || normalized.contains("nen mua")
                || normalized.contains("dang mua")
                || normalized.contains("da mua")
                || normalized.contains("phan van")
                || normalized.contains("tinh dat sau")
                || normalized.contains("neu con hang")
                || normalized.contains("lam sao")
                || normalized.contains("hoi cach")
                || normalized.contains("cach huy")
                || normalized.contains("cach dat")
                || normalized.contains("xem thu")
                || (normalized.contains("tim") && normalized.contains("dat") && normalized.contains("phu hop"));
    }

    @FunctionalInterface
    private interface LightWriteExecutor {
        AgentResponse execute(Long userId, ActionPayload payload);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ActionPayload(
            Long bookId,
            String bookTitle,
            int quantity,
            String couponCode,
            String paymentMethod,
            String shippingAddress,
            String customerPhone,
            List<Long> selectedBookIds,
            Long orderId,
            BigDecimal unitPrice,
            boolean cartCheckout
    ) {
    }

    private record BookReference(Long id, String title) {
    }
}
