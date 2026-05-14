package iuh.fit.se.modules.ai.application.service;

import iuh.fit.se.modules.ai.application.port.in.AiChatUseCase;
import iuh.fit.se.modules.ai.application.port.out.ChatHistoryPersistencePort;
import iuh.fit.se.modules.ai.application.port.out.LlmPort;
import iuh.fit.se.modules.ai.application.port.out.VectorStorePort;
import iuh.fit.se.modules.ai.domain.ChatMessage;
import iuh.fit.se.modules.ai.domain.ChatRole;
import iuh.fit.se.modules.ai.domain.ChatSession;
import iuh.fit.se.modules.catalog.application.port.in.BookDTO;
import iuh.fit.se.modules.catalog.application.port.in.BookUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService implements AiChatUseCase {

    private static final int RAG_TOP_K = 5;
    private static final Pattern REQUESTED_BOOK_COUNT_PATTERN = Pattern.compile("\\b(\\d{1,2})\\b");

    static final String FALLBACK_RESPONSE = "Xin lỗi, SEBook Assistant đang tạm thời không kết nối được AI. "
            + "Bạn vui lòng thử lại sau ít phút nhé.";

    private final LlmPort llmPort;
    private final ChatHistoryPersistencePort historyPort;
    private final VectorStorePort vectorStorePort;
    private final BookUseCase bookUseCase;

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

        // 2. Retrieve relevant catalog data and call LLM with previous history.
        List<BookDTO> relevantBooks = findRelevantBooks(message);
        int requestedBookCount = resolveRequestedBookCount(message);
        String prompt = buildPromptWithCatalogContext(message, relevantBooks, requestedBookCount);
        String response;
        try {
            response = llmPort.chat(prompt, previousMessages);
        } catch (Exception e) {
            log.error("AI chat failed for sessionId: {}", sessionId, e);
            response = buildCatalogFallbackResponse(relevantBooks, requestedBookCount);
        }

        // 3. Save assistant response
        ChatMessage assistantMsg = ChatMessage.create(sessionId, ChatRole.ASSISTANT, response);
        session.markActive();
        historyPort.saveSession(session);
        historyPort.saveMessage(assistantMsg);

        return response;
    }

    private String buildPromptWithCatalogContext(String userMessage, List<BookDTO> books, int requestedBookCount) {
        if (books.isEmpty()) {
            return userMessage;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là SEBook Assistant, trợ lý bán sách của nhà sách SEBook.\n");
        prompt.append("Chỉ đưa ra gợi ý dựa trên KHO SÁCH SEBook bên dưới. ");
        prompt.append("Nếu kho sách không đủ dữ liệu để trả lời, hãy nói rõ và đề xuất người dùng tìm thêm trên trang danh sách sách.\n\n");
        prompt.append("[KHO SÁCH SEBook]\n");

        for (int index = 0; index < books.size(); index++) {
            BookDTO book = books.get(index);
            prompt.append(index + 1).append(". ");
            prompt.append(nullToFallback(book.title(), "Không rõ tên sách"));
            prompt.append(" - Tác giả: ").append(nullToFallback(book.author(), "Không rõ"));
            prompt.append(" - Giá: ").append(formatPrice(book.price()));
            prompt.append(" - Tồn kho: ").append(book.quantity() > 0 ? "Còn hàng: " + book.quantity() : "Hết hàng");
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
        prompt.append("Khách hàng yêu cầu tối đa ").append(requestedBookCount)
                .append(" cuốn, nên chỉ gợi ý đúng số lượng này nếu kho sách có đủ.");

        return prompt.toString();
    }

    private String buildCatalogFallbackResponse(List<BookDTO> books, int requestedBookCount) {
        if (books.isEmpty()) {
            return FALLBACK_RESPONSE;
        }

        List<BookDTO> limitedBooks = books.stream()
                .limit(requestedBookCount)
                .toList();

        StringBuilder response = new StringBuilder();
        response.append("Mình tìm được một số sách phù hợp trong kho SEBook cho bạn:\n");
        for (int index = 0; index < limitedBooks.size(); index++) {
            BookDTO book = limitedBooks.get(index);
            response.append(index + 1).append(". ");
            response.append(nullToFallback(book.title(), "Không rõ tên sách"));
            response.append(" - ").append(nullToFallback(book.author(), "Không rõ tác giả"));
            response.append(" - ").append(book.quantity() > 0 ? "Còn hàng: " + book.quantity() : "Hết hàng");
            if (book.description() != null && !book.description().isBlank()) {
                response.append("\n   ").append(truncate(book.description(), 180));
            }
            response.append("\n");
        }
        response.append("Bạn có thể bấm vào kết quả tìm kiếm hoặc tìm tên sách trên trang danh sách sách để xem chi tiết.");
        return response.toString();
    }

    private int resolveRequestedBookCount(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return RAG_TOP_K;
        }

        Matcher matcher = REQUESTED_BOOK_COUNT_PATTERN.matcher(userMessage);
        if (matcher.find()) {
            return clampBookCount(Integer.parseInt(matcher.group(1)));
        }

        String normalized = Normalizer.normalize(userMessage.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        if (normalized.contains("mot ")) {
            return 1;
        }
        if (normalized.contains("hai ")) {
            return 2;
        }
        if (normalized.contains("ba ")) {
            return 3;
        }
        if (normalized.contains("bon ") || normalized.contains("tu ")) {
            return 4;
        }
        if (normalized.contains("nam ")) {
            return 5;
        }
        return RAG_TOP_K;
    }

    private int clampBookCount(int count) {
        if (count < 1) {
            return 1;
        }
        return Math.min(count, RAG_TOP_K);
    }

    private List<BookDTO> findRelevantBooks(String userMessage) {
        try {
            List<Long> bookIds = vectorStorePort.findSimilarBooks(userMessage, RAG_TOP_K);
            if (bookIds == null || bookIds.isEmpty()) {
                return List.of();
            }
            return bookIds.stream()
                    .map(this::findBookSafely)
                    .filter(Objects::nonNull)
                    .filter(BookDTO::isActive)
                    .toList();
        } catch (Exception e) {
            log.warn("Unable to retrieve AI catalog context for prompt: {}", userMessage, e);
            return List.of();
        }
    }

    private BookDTO findBookSafely(Long bookId) {
        try {
            return bookUseCase.getBook(bookId);
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
