package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.AiChatUseCase;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort;
import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookContext;
import iuh.fit.se.modules.ai.application.port.out.ChatHistoryPersistencePort;
import iuh.fit.se.modules.ai.application.port.out.LlmPort;
import iuh.fit.se.modules.ai.application.port.out.SalesRankingPort;
import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import iuh.fit.se.modules.ai.domain.ChatRole;
import iuh.fit.se.modules.ai.domain.ChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService implements AiChatUseCase {

    private static final int RAG_TOP_K = 5;

    static final String FALLBACK_RESPONSE = "Xin lỗi, SEBook Assistant đang tạm thời không kết nối được AI. "
            + "Bạn vui lòng thử lại sau ít phút nhé.";

    private final LlmPort llmPort;
    private final ChatHistoryPersistencePort historyPort;
    private final VectorStorePort vectorStorePort;
    private final CatalogBookPort catalogBookPort;
    private final SalesRankingPort salesRankingPort;

    @Override
    public String chat(String sessionId, Long customerId, String message) {
        ChatSession session = historyPort.findById(sessionId)
                .orElseGet(() -> {
                    ChatSession newSession = ChatSession.create(sessionId, customerId);
                    historyPort.saveSession(newSession);
                    return newSession;
                });

        List<ChatMessage> previousMessages = historyPort.findMessagesBySessionId(sessionId);

        // 1. Save user message
        ChatMessage userMsg = ChatMessage.create(sessionId, ChatRole.USER, message);
        session.markActive();
        historyPort.saveSession(session);
        historyPort.saveMessage(userMsg);

        String faqResponse = naturalFaqResponse(message);
        if (faqResponse != null) {
            saveAssistantMessage(session, sessionId, faqResponse);
            return faqResponse;
        }

        // 2. Retrieve relevant catalog data and call LLM with previous history.
        int requestedBookCount = resolveRequestedBookCount(message);
        boolean hasRequestedBookCount = AiCatalogMatchSupport.hasRequestedBookCount(message);
        List<BookContext> relevantBooks = findRelevantBooks(message, requestedBookCount);
        String prompt = buildPromptWithCatalogContext(message, relevantBooks, requestedBookCount, hasRequestedBookCount);
        String response;
        if (!relevantBooks.isEmpty() && shouldAnswerFromCatalog(message)) {
            response = buildCatalogFallbackResponse(relevantBooks, requestedBookCount, hasRequestedBookCount);
        } else if (relevantBooks.isEmpty()
                && (shouldAnswerFromCatalog(message) || AiCatalogMatchSupport.hasStrictTopic(message))) {
            response = "Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.";
        } else {
            try {
                response = llmPort.chat(prompt, previousMessages);
            } catch (Exception e) {
                log.error("AI chat failed for sessionId: {}", sessionId, e);
                response = buildCatalogFallbackResponse(relevantBooks, requestedBookCount, hasRequestedBookCount);
            }
        }

        // 3. Save assistant response
        ChatMessage assistantMsg = ChatMessage.create(sessionId, ChatRole.ASSISTANT, response);
        session.markActive();
        historyPort.saveSession(session);
        historyPort.saveMessage(assistantMsg);

        return response;
    }

    private String naturalFaqResponse(String message) {
        String normalized = AiCatalogMatchSupport.normalizeText(message);
        if (normalized.matches("^(xin chao|chao|hello|hi)( ban| shop| sebook)?[ !?.]*$")) {
            return "Xin ch\u00e0o! M\u00ecnh l\u00e0 SEBook Assistant. M\u00ecnh c\u00f3 th\u1ec3 gi\u00fap b\u1ea1n t\u00ecm s\u00e1ch, t\u01b0 v\u1ea5n ch\u1ecdn s\u00e1ch ho\u1eb7c h\u01b0\u1edbng d\u1eabn \u0111\u1eb7t h\u00e0ng.";
        }
        if ((normalized.contains("lam sao") || normalized.contains("cach") || normalized.contains("huong dan"))
                && normalized.contains("dat hang")) {
            return "B\u1ea1n ch\u1ecdn s\u00e1ch, th\u00eam v\u00e0o gi\u1ecf h\u00e0ng, m\u1edf gi\u1ecf v\u00e0 nh\u1ea5n \u0111\u1eb7t h\u00e0ng. Sau \u0111\u00f3 b\u1ea1n nh\u1eadp \u0111\u1ecba ch\u1ec9 giao h\u00e0ng, s\u1ed1 \u0111i\u1ec7n tho\u1ea1i v\u00e0 ch\u1ecdn ph\u01b0\u01a1ng th\u1ee9c thanh to\u00e1n.";
        }
        if (normalized.contains("sach nao") && normalized.contains("phu hop cho sinh vien")) {
            return "M\u00ecnh c\u00f3 th\u1ec3 g\u1ee3i \u00fd s\u00e1ch cho sinh vi\u00ean. B\u1ea1n \u0111ang h\u1ecdc ng\u00e0nh n\u00e0o ho\u1eb7c mu\u1ed1n \u0111\u1ecdc ch\u1ee7 \u0111\u1ec1 g\u00ec, v\u00ed d\u1ee5 l\u1eadp tr\u00ecnh, kinh t\u1ebf hay k\u1ef9 n\u0103ng s\u1ed1ng?";
        }
        return null;
    }

    private void saveAssistantMessage(ChatSession session, String sessionId, String response) {
        ChatMessage assistantMsg = ChatMessage.create(sessionId, ChatRole.ASSISTANT, response);
        session.markActive();
        historyPort.saveSession(session);
        historyPort.saveMessage(assistantMsg);
    }

    private String buildPromptWithCatalogContext(
            String userMessage,
            List<BookContext> books,
            int requestedBookCount,
            boolean hasRequestedBookCount
    ) {
        if (books.isEmpty()) {
            return userMessage;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là SEBook Assistant, trợ lý bán sách của nhà sách SEBook.\n");
        prompt.append("Chỉ đưa ra gợi ý dựa trên KHO SÁCH SEBook bên dưới. ");
        prompt.append("Nếu kho sách không đủ dữ liệu để trả lời, hãy nói rõ và đề xuất người dùng tìm thêm trên trang danh sách sách.\n\n");
        prompt.append("[KHO SÁCH SEBook]\n");

        for (int index = 0; index < books.size(); index++) {
            BookContext book = books.get(index);
            prompt.append(index + 1).append(". ");
            prompt.append(nullToFallback(book.title(), "Không rõ tên sách"));
            prompt.append(" - Tác giả: ").append(nullToFallback(book.author(), "Không rõ"));
            prompt.append(" - Giá: ").append(formatPrice(book.price()));
            prompt.append(" - Tồn kho: ").append(book.quantity() > 0 ? "Còn hàng: " + book.quantity() : "Hết hàng");
            prompt.append(" - Link: ").append(AiCatalogMatchSupport.productLink(book.id()));
            if (book.categoryNames() != null && !book.categoryNames().isEmpty()) {
                prompt.append(" - Danh mục: ").append(String.join(", ", book.categoryNames()));
            }
            if (book.averageRating() != null) {
                prompt.append(" - Đánh giá: ").append(book.averageRating());
                if (book.ratingCount() > 0) {
                    prompt.append(" (").append(book.ratingCount()).append(" lượt)");
                }
            }
            if (book.description() != null && !book.description().isBlank()) {
                prompt.append("\n   Mô tả: ").append(truncate(book.description(), 360));
            }
            if (book.keywords() != null && !book.keywords().isEmpty()) {
                prompt.append("\n   Từ khóa: ").append(String.join(", ", book.keywords()));
            }
            prompt.append("\n");
        }

        prompt.append("\n[Câu hỏi khách hàng]\n");
        prompt.append(userMessage);
        prompt.append("\n\nHãy trả lời bằng tiếng Việt có dấu, ngắn gọn, đúng vai trò tư vấn bán sách. ");
        prompt.append("Nếu gợi ý sách, hãy nêu tên sách, tác giả và lý do phù hợp. ");
        prompt.append("Luôn kèm link sản phẩm dạng /books/{id} cho mỗi sách được gợi ý. ");
        prompt.append("Khách hàng yêu cầu tối đa ").append(requestedBookCount)
                .append(" cuốn, nên chỉ gợi ý đúng số lượng này nếu kho sách có đủ.");
        if (hasRequestedBookCount && books.size() < requestedBookCount) {
            prompt.append(" Hiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn, hãy nói rõ câu này trong câu trả lời.");
        }

        return prompt.toString();
    }

    private boolean shouldAnswerFromCatalog(String userMessage) {
        String normalized = AiCatalogMatchSupport.normalizeText(userMessage);
        return AiCatalogMatchSupport.hasTitleSearchIntent(userMessage)
                || AiCatalogMatchSupport.hasAuthorSearchIntent(userMessage)
                || AiCatalogMatchSupport.hasAudienceSearchIntent(userMessage)
                || AiCatalogMatchSupport.hasRankingIntent(userMessage)
                || AiCatalogMatchSupport.hasStrictTopic(userMessage)
                || AiCatalogMatchSupport.hasCatalogIntentKeywords(userMessage)
                || normalized.contains("goi y")
                || normalized.contains("tim sach")
                || normalized.contains("sach nao")
                || normalized.contains("cuon nao")
                || normalized.contains("quyen nao");
    }

    private String buildCatalogFallbackResponse(
            List<BookContext> books,
            int requestedBookCount,
            boolean hasRequestedBookCount
    ) {
        if (books.isEmpty()) {
            return FALLBACK_RESPONSE;
        }

        List<BookContext> limitedBooks = books.stream()
                .limit(requestedBookCount)
                .toList();

        StringBuilder response = new StringBuilder();
        response.append("Mình tìm được một số sách phù hợp trong kho SEBook cho bạn:\n");
        for (int index = 0; index < limitedBooks.size(); index++) {
            BookContext book = limitedBooks.get(index);
            response.append(index + 1).append(". ");
            response.append(nullToFallback(book.title(), "Không rõ tên sách"));
            response.append(" - ").append(nullToFallback(book.author(), "Không rõ tác giả"));
            response.append(" - ").append(book.quantity() > 0 ? "Còn hàng: " + book.quantity() : "Hết hàng");
            response.append(" - Link: ").append(AiCatalogMatchSupport.productLink(book.id()));
            if (book.description() != null && !book.description().isBlank()) {
                response.append("\n   ").append(truncate(book.description(), 180));
            }
            response.append("\n");
        }
        response.append("Bạn có thể bấm vào kết quả tìm kiếm hoặc tìm tên sách trên trang danh sách sách để xem chi tiết.");
        if (hasRequestedBookCount && limitedBooks.size() < requestedBookCount) {
            response.append("\nHiện tại chúng tôi không có đủ sản phẩm cho yêu cầu của bạn.");
        }
        return response.toString();
    }

    private int resolveRequestedBookCount(String userMessage) {
        return AiCatalogMatchSupport.resolveRequestedBookCount(userMessage, RAG_TOP_K);
    }

    private List<BookContext> findRelevantBooks(String userMessage, int requestedBookCount) {
        try {
            if (AiCatalogMatchSupport.hasRankingIntent(userMessage)) {
                return salesRankingPort.getTopSellingBooks(requestedBookCount);
            }

            List<Long> bookIds = vectorStorePort.findSimilarBooks(userMessage, RAG_TOP_K);
            if (bookIds == null || bookIds.isEmpty()) {
                return supplementWithCatalogCategoryBooks(userMessage, List.of(), requestedBookCount);
            }
            List<BookContext> books = bookIds.stream()
                    .map(this::findBookSafely)
                    .filter(Objects::nonNull)
                    .filter(BookContext::isActive)
                    .toList();
            List<BookContext> matchedBooks = AiCatalogMatchSupport.keepOnlyExplicitTopicMatches(userMessage, books);
            matchedBooks = AiCatalogMatchSupport.keepOnlyCatalogIntentKeywordMatches(userMessage, matchedBooks);
            return supplementWithCatalogCategoryBooks(userMessage, matchedBooks, requestedBookCount);
        } catch (Exception e) {
            log.warn("Unable to retrieve AI catalog context for prompt: {}", userMessage, e);
            return List.of();
        }
    }

    private List<BookContext> supplementWithCatalogCategoryBooks(
            String userMessage,
            List<BookContext> currentBooks,
            int requestedBookCount
    ) {
        if (currentBooks.size() >= requestedBookCount) {
            return currentBooks;
        }
        List<String> topics = AiCatalogMatchSupport.categoryTopicsIn(userMessage);
        if (topics.isEmpty()) {
            return currentBooks;
        }

        Map<Long, BookContext> booksById = new LinkedHashMap<>();
        currentBooks.stream()
                .filter(book -> book.id() != null)
                .forEach(book -> booksById.put(book.id(), book));

        for (String topic : topics) {
            List<BookContext> categoryBooks = catalogBookPort.searchBooksByCategoryName(topic);
            if (categoryBooks == null || categoryBooks.isEmpty()) {
                continue;
            }
            categoryBooks.stream()
                    .filter(Objects::nonNull)
                    .filter(book -> book.id() != null)
                    .filter(BookContext::isActive)
                    .forEach(book -> booksById.putIfAbsent(book.id(), book));
            if (booksById.size() >= requestedBookCount) {
                break;
            }
        }

        return booksById.values().stream().toList();
    }

    private BookContext findBookSafely(Long bookId) {
        try {
            return catalogBookPort.getBook(bookId);
        } catch (Exception e) {
            log.warn("Unable to load book {} for AI chat context", bookId, e);
            return null;
        }
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "Chua cap nhat";
        }
        return price.stripTrailingZeros().toPlainString() + " VND";
    }

    private String nullToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    @Override
    @Transactional
    public void clearHistory(String sessionId) {
        historyPort.deleteSession(sessionId);
    }
}
